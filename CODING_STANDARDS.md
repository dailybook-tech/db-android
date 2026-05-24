# DailyBook Android — Coding Standards

This document defines the coding standards, architectural conventions, and contribution guidelines
for the DailyBook Android project. It serves as a binding reference for both human developers and
AI coding agents making changes to this codebase.

---

## Table of Contents

1. [Architecture Rules](#architecture-rules)
2. [Module Structure](#module-structure)
3. [Naming Conventions](#naming-conventions)
4. [File Organization](#file-organization)
5. [Kotlin Style](#kotlin-style)
6. [MVVM & State Management](#mvvm--state-management)
7. [Dependency Injection (Koin)](#dependency-injection-koin)
8. [Networking](#networking)
9. [Data Layer](#data-layer)
10. [UI & Layouts](#ui--layouts)
11. [Navigation](#navigation)
12. [Analytics](#analytics)
13. [Testing](#testing)
14. [Git & PR Guidelines](#git--pr-guidelines)
15. [AI Agent Rules](#ai-agent-rules)

---

## Architecture Rules

### Layered Architecture (strict)

Every feature follows this layered structure. Dependencies flow **downward only**.

```
Fragment/Activity (View)
    ↓ observes LiveData
ViewModel
    ↓ calls
UseCase (interface + impl)
    ↓ calls
Repository (interface + impl)
    ↓ calls
NetworkModule / DAO
    ↓ calls
Retrofit API / Room Database
```

**Rules:**
- Views MUST NOT call repositories or network modules directly.
- ViewModels MUST NOT reference Android framework classes (`Context`, `View`, `Fragment`).
- UseCases encapsulate a single business operation. One UseCase per operation.
- Repositories abstract data sources. A ViewModel never knows if data came from network or cache.
- NetworkModules handle the Retrofit call, error mapping, and caching orchestration.

### No Circular Dependencies

Module A must not depend on Module B if Module B already depends on Module A. The dependency
graph is strictly acyclic. Check `build.gradle` `implementation project(...)` lines before adding
a new module dependency.

---

## Module Structure

### When to Create a New Feature Module

Create a new `feature/` module when:
- The feature has its own distinct API surface (new endpoints)
- It has 3+ screens with unique view models
- It could conceptually be removed without breaking other features

### Feature Module Template

Every feature module follows this package layout:

```
com.dailybook.<feature>/
├── di/
│   └── <Feature>Module.kt        # Koin module
├── model/
│   ├── request/                   # API request bodies
│   └── response/                  # API response types (if distinct from domain models)
├── network/
│   ├── <Feature>Api.kt            # Retrofit interface
│   └── <Feature>NetworkModule.kt  # Network call wrappers
├── repository/
│   ├── <Feature>Repository.kt     # Interface
│   └── <Feature>RepositoryImpl.kt # Implementation
├── usecase/
│   ├── <UseCase>UseCase.kt        # Interface
│   └── <UseCase>UseCaseImpl.kt    # Implementation
├── screen/
│   └── <screenname>/
│       ├── view/
│       │   └── <Screen>Fragment.kt
│       ├── viewmodel/
│       │   └── <Screen>ViewModel.kt
│       └── uistate/
│           └── UiState.kt
└── util/                          # Feature-specific utilities
```

### Boilerplate Modules

`boilerplate/` modules contain shared, feature-agnostic code. They MUST NOT depend on any
`feature/` module. Changes to boilerplate modules affect the entire app — exercise extra caution
and test thoroughly.

---

## Naming Conventions

### Packages

| Type | Convention | Example |
|------|-----------|---------|
| Feature package | `com.dailybook.<feature>` | `com.dailybook.keep` |
| Boilerplate package | `com.boilerplate.<library>` | `com.boilerplate.network` |

### Classes

| Type | Convention | Example |
|------|-----------|---------|
| Activity | `<Name>Activity` | `BookKeepActivity` |
| Fragment | `<Name>Fragment` | `StaffListFragment` |
| Bottom sheet | `<Name>BottomSheetFragment` or `<Name>BottomsheetFragment` | `AttendanceMarkBottomsheetFragment` |
| ViewModel | `<Name>ViewModel` | `CalendarViewModel` |
| UseCase interface | `<Action>UseCase` | `GetTransactionsUseCase` |
| UseCase impl | `<Action>UseCaseImpl` or `<Action>UseCaseImplementation` | `GetTransactionsUseCaseImpl` |
| Repository interface | `<Feature>Repository` | `KeepRepository` |
| Repository impl | `<Feature>RepositoryImpl` or `<Feature>RepositoryImplementation` | `KeepRepositoryImplementation` |
| Retrofit API | `<Feature>Api` | `KeepApi`, `AuthApi` |
| Network module | `<Feature>NetworkModule` | `KeepNetworkModule` |
| Koin module | `<feature>Module` (val) | `val keepModule = module { ... }` |
| Room entity | Domain name, annotated `@Entity(tableName = "...")` | `StaffUser` |
| Room DAO | `<Entity>Dao` | `StaffUserDao` |
| Room database | `<Purpose>Database` | `AppDatabase` |
| RecyclerView adapter | `<Item>Adapter` | `StaffUserAdapter` |
| Broadcast receiver | `<Purpose>Receiver` | `BootReceiver` |
| Request body | `<Action>RequestBody` | `MarkSingleAttendanceRequestBody` |
| Response model | `<Action>Response` or `<Action>ResponseModel` | `AuthResponse` |
| UI state | `UiState` (sealed class) | `sealed class UiState` |
| Constants | `<Feature>Constants` or specific object | `BaseConstants`, `AdUnitConstants` |

### Functions

| Type | Convention | Example |
|------|-----------|---------|
| ViewModel actions | verb-first | `loadStaffList()`, `markAttendance()` |
| UseCase entry | `invoke()` (operator) or descriptive | `operator fun invoke()` |
| Repository methods | verb matching API action | `getStaffUsers()`, `createTransaction()` |
| Extension functions | descriptive, on receiver type | `Fragment.hideKeyboard()` |
| Event handlers | `on<Event>` | `onStaffClicked()` |

### Resources

| Type | Convention | Example |
|------|-----------|---------|
| Layout (activity) | `activity_<name>.xml` | `activity_book_keep.xml` |
| Layout (fragment) | `fragment_<name>.xml` | `fragment_staff_list.xml` |
| Layout (item) | `item_<name>.xml` | `item_contact_list.xml` |
| Layout (dialog) | `dialog_<name>.xml` | `dialog_premium_offer.xml` |
| Layout (bottom sheet) | `fragment_<name>_bottomsheet.xml` or `bottomsheet_<name>.xml` | `fragment_attendance_mark_bottomsheet.xml` |
| Strings | `snake_case` | `@string/staff_list_title` |
| Colors | `snake_case` | `@color/primary_green` |
| Dimens | `<component>_<property>_<size>` | `@dimen/button_padding_horizontal` |
| Drawable | `ic_<name>` for icons, `bg_<name>` for backgrounds | `ic_calendar`, `bg_rounded_card` |

### Variables

- `camelCase` for local variables and properties
- `SCREAMING_SNAKE_CASE` for constants and `companion object` vals
- Prefix private backing properties with underscore: `private val _uiState`
- No Hungarian notation (`mVariable`, `sInstance`)

---

## File Organization

### Imports
- No wildcard imports (`import com.example.*`)
- Group: Android SDK → third-party libraries → project modules (IDE auto-sort is fine)
- Remove unused imports

### Class Structure (top to bottom)
1. Companion object / constants
2. Injected dependencies (constructor or `by inject()`)
3. Private properties and backing fields
4. Lifecycle overrides (`onCreate`, `onViewCreated`, `onResume`, `onDestroyView`)
5. Public/internal methods
6. Private helper methods
7. Inner classes / sealed classes

### One Public Class Per File
Match filename to the primary public class. Small related data classes (e.g., request/response pairs)
may share a file if they form a cohesive unit.

---

## Kotlin Style

### General
- Use `val` over `var` wherever possible
- Prefer `data class` for models and state
- Use `sealed class` or `sealed interface` for exhaustive state and UI events
- Use `when` with exhaustive branches on sealed types (no `else` branch)
- Use expression bodies for single-expression functions: `fun isActive() = status == ACTIVE`
- Use `?.let { }` and `?.also { }` over explicit null checks when it improves readability
- Avoid `!!` — handle nullability explicitly. If a non-null assertion is truly necessary, add a comment explaining why

### Coroutines
- Launch coroutines in `viewModelScope` from ViewModels
- Use `Dispatchers.IO` for network/database work (within `withContext` or in the repository layer)
- Use `Flow` for reactive data streams from repositories
- Collect Flows in `lifecycleScope.launchWhenStarted` or `repeatOnLifecycle(STARTED)` from Fragments
- Never use `GlobalScope`

### Extensions
- Place widely-used extensions in `feature/base/.../BaseExtension.kt`
- Place feature-specific extensions in the feature's `util/` package
- Keep extension functions focused — one concern per function

---

## MVVM & State Management

### UiState Pattern

Every screen with asynchronous loading uses a sealed `UiState` class:

```kotlin
sealed class UiState {
    object Loading : UiState()
    data class Success(val data: SomeModel) : UiState()
    data class Error(val message: String) : UiState()
    object Empty : UiState()
}
```

### ViewModel Rules
- Extend `BaseViewModel<UiState>` when possible
- Expose state via `LiveData` (the project uses LiveData, not StateFlow — stay consistent until a migration is explicitly planned)
- Keep ViewModels thin — delegate business logic to UseCases
- One ViewModel per screen; share via `by sharedViewModel()` only within the same activity scope
- Never hold references to Views, Fragments, or Activities

### Fragment Rules
- Use ViewBinding (`FragmentXBinding.inflate()`) — never `findViewById`
- Null out binding in `onDestroyView` (handled by `BaseFragment`)
- Observe ViewModel state in `onViewCreated`
- Delegate click handling to ViewModel methods

---

## Dependency Injection (Koin)

### Module Registration
- Each feature has exactly one Koin module file in its `di/` package
- Register: `single { }` for singletons (repositories, network modules, managers)
- Register: `viewModel { }` for ViewModels
- Register: `factory { }` for stateless use cases

### Injection
- In Fragments/Activities: `by inject()` for singletons, `by viewModel()` for ViewModels
- In classes not managed by Koin: pass dependencies via constructor
- Never call `get()` inline — prefer constructor injection or `by inject()` delegation

### Adding New Modules
When creating a new feature module:
1. Create `<Feature>Module.kt` in the `di/` package
2. Add it to the `startKoin` block in `MainApp.kt`
3. Follow the existing module patterns for consistency

---

## Networking

### API Interface
- One Retrofit interface per feature module (`<Feature>Api.kt`)
- Use `@GET`, `@POST`, `@PUT`, `@DELETE`, `@PATCH` annotations
- Return `Flow<DataResponse<T>>` using the Flow call adapter
- Use `@Body` for request bodies, `@Path` for URL segments, `@Query` for query params

### NetworkModule
- Wraps API calls with error handling and optional caching
- Uses `NetworkHandler` from the boilerplate for cache-first patterns
- All HTTP errors should be caught and mapped to domain errors before reaching the ViewModel

### Headers
Auth headers are injected automatically via OkHttp interceptors in `NetworkHandler`. Do not
manually add auth headers to individual API calls.

### Error Handling
- Network errors surface as `NetworkResult.ERROR` with a message
- Token expiration (401) triggers automatic refresh via `AuthRepositoryImpl`
- No raw exceptions should propagate to the UI layer

---

## Data Layer

### Room
- Use `@Entity` with explicit `tableName`
- Always define a `@PrimaryKey`
- Use `fallbackToDestructiveMigration()` only during early development; write proper migrations for production schema changes
- Cap unbounded tables (e.g., `CalendarItem` is capped at 20,000 rows)
- DAOs return `Flow` or `List` — avoid `LiveData` in DAOs (convert at the ViewModel layer)

### DataStore
- All preference keys live in `DataStoreManager`
- Access via `DataStoreManager` extension functions
- Never use `SharedPreferences` — the project has fully migrated to DataStore
- Use typed accessors; don't scatter raw key strings across the codebase

---

## UI & Layouts

### XML Layouts
- Use `ConstraintLayout` as the primary layout manager
- Avoid deep nesting (max 3 levels of nested `ViewGroup`s)
- Use `@dimen` resources for margins and padding — avoid hardcoded `dp` values
- Use `@color` and `@style` resources — avoid inline hex colors
- Prefer `Material` components (`MaterialButton`, `MaterialCardView`, `TextInputLayout`)

### Design System (boilerplate/uikit)
- Use `PrimaryButton`, `ActionButton`, etc. from the uikit for consistency
- Use the pre-themed `TextView*` variants (`TextViewBold16`, `TextViewRegular14`, etc.)
- Custom views should extend from existing uikit components when possible

### Accessibility
- All clickable views must have `contentDescription`
- Use `importantForAccessibility` to hide decorative elements
- Test with TalkBack before releasing new UI features

### Localization
- All user-visible strings go in `strings.xml`
- Never hardcode strings in Kotlin or XML layouts
- When adding a new string, add translations for all supported locales (en, hi, ta, te, kn, mr, bn)
- Use plurals (`<plurals>`) for count-dependent text

---

## Navigation

### Activity Navigation (ModuleNavigator)
- Cross-module navigation uses `ModuleNavigator` with `ActivitiesNameEnum`
- Activity class names are resolved via `AddressGenerator`
- Always use `ModuleNavigator` — never hardcode `Intent(context, SomeActivity::class.java)` across
  module boundaries

### Fragment Navigation (FragmentNavigator)
- Within a feature, use `fragmentNavigator.start(Fragment)` to push onto the current back stack
- Use `fragmentNavigator.goBack()` to pop
- Bottom sheets shown via navigator are part of the back stack
- Dialog-style bottom sheets (premium offer, month chooser) use `.show(parentFragmentManager, TAG)`

### Deep Links
- Currently not implemented. If adding deep links, use the `ModuleNavigator` pattern with a
  centralized routing handler in `RoutingActivity`.

---

## Analytics

### Event Tracking
- Every screen MUST set `screenName` in its Fragment for automatic impression tracking
- Use the `Analytics` class (injected via Koin) for all event logging
- Event names go in `ConstantEventNames`; attribute keys in `ConstantEventAttributes`
- Never log PII (phone numbers, names) in analytics events

### Adding New Events
1. Add the event name constant to `ConstantEventNames`
2. Add attribute key constants to `ConstantEventAttributes` if needed
3. Call `analytics.track(eventName, properties)` from the Fragment or ViewModel

---

## Testing

### Unit Tests (required for all new code)
- Test UseCases and Repositories at minimum
- Use Mockito or MockK for mocking dependencies
- Test file naming: `<ClassName>Test.kt` in the matching `src/test/` directory
- Test structure: Arrange-Act-Assert with descriptive function names:
  `fun 'should return error when network fails'()`

### Instrumented Tests
- Located in `app/src/androidTest/`
- Use UiAutomator for end-to-end flows
- Keep test data deterministic (use test backend or mocked responses)

### What to Test
| Layer | Must test | Optional |
|-------|-----------|----------|
| UseCase | All branches of `invoke()` | Edge cases |
| Repository | Data source switching, error mapping | Caching behavior |
| ViewModel | State transitions for all UiState variants | UI event emissions |
| Fragment | — | Navigation side effects |

---

## Git & PR Guidelines

### Branch Naming
```
feature/<ticket-or-short-description>   # New features
fix/<ticket-or-short-description>       # Bug fixes
refactor/<description>                  # Refactoring
chore/<description>                     # Build, CI, tooling
```

### Commit Messages
Use conventional commits:
```
feat: add team management screen
fix: prevent crash on empty staff list
refactor: extract attendance marking to use case
chore: update Gradle to 8.9
docs: update claude.md with subscription flow
```

### Pull Request Checklist
- [ ] Code follows this standards document
- [ ] New strings are localized in all supported languages
- [ ] No hardcoded colors, dimensions, or strings
- [ ] Unit tests added/updated for new business logic
- [ ] No `!!` usage without a justifying comment
- [ ] No new lint warnings introduced
- [ ] ViewModels have no Android framework imports
- [ ] Analytics events tracked for new user-facing features
- [ ] Tested on minSdk device (API 24)

### Code Review Focus Areas
- Architecture layer violations (View → Repository direct call)
- Memory leaks (context references in ViewModels, un-nulled bindings)
- Thread safety (Dispatchers usage, coroutine scope)
- Backwards compatibility (minSdk 24)

---

## AI Agent Rules

These rules are specifically for AI coding agents (Cursor, Claude, etc.) making changes to this
codebase. Follow them strictly.

### Before Making Changes
1. **Read `claude.md` first** — understand the app architecture, module structure, and data flow
2. **Identify the correct module** — changes to staff/attendance go in `feature/keep`, transactions
   in `feature/income` or `feature/expense`, shared utilities in `feature/base`
3. **Check existing patterns** — look at how similar features are implemented before writing new code.
   This project has strong consistency; deviate only with explicit justification

### Code Changes
1. **Follow the layer cake** — never shortcut the architecture (e.g., calling an API from a Fragment)
2. **Match existing style exactly** — if the module uses `Implementation` suffix, don't introduce
   `Impl`. If it uses `LiveData`, don't introduce `StateFlow` without approval
3. **No new dependencies without approval** — don't add libraries. Use what's already in
   `libs.versions.toml`
4. **No Jetpack Compose** — this is a View-based project. Do not introduce Compose unless explicitly
   directed to
5. **Use Koin, not Hilt** — the project uses Koin for DI. Never add Dagger/Hilt annotations
6. **Respect module boundaries** — never import from a sibling feature module (e.g., `income` must
   not import from `expense`). Shared code goes in `feature/base` or `boilerplate/`
7. **Preserve all existing analytics** — when modifying screens, keep existing analytics event calls
   intact. Add new events for new interactions

### Naming
- All new files, classes, packages, and references must use `dailybook` / `DailyBook` naming
- When modifying existing files, update any legacy naming found within the scope of your change
  to use `dailybook` / `DailyBook`
- A full codebase-wide rename is a separate dedicated task — do not attempt a sweeping rename as
  a side effect of other changes

### What NOT to Do
- Do not delete or consolidate the income and expense modules (they are intentionally separate
  despite being structurally identical)
- Do not move from Room to another database without explicit direction
- Do not introduce SharedPreferences (project uses DataStore exclusively)
- Do not remove or modify ProGuard rules without understanding the downstream impact
- Do not modify `boilerplate/` modules for feature-specific logic
- Do not modify `google-services.json`, keystore files, or signing configs
- Do not change `minSdk`, `targetSdk`, `compileSdk`, or Kotlin/Gradle versions without approval
- Do not add code comments that merely narrate what the code does

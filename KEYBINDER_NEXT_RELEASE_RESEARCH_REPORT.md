# Keybinder: исследование следующего релиза

Дата: 2026-08-01  
База исследования: текущий исходный код Keybinder, `RESEARCH_REPORT.md` и `libs/client-patched.jar`.

## Итог

Все восемь направлений реализуемы на текущем клиенте без возвращения постоянных legacy-команд и без разрушительной миграции пользовательских биндов.

Главное архитектурное изменение нужно формулировать не как «добавить ещё один target», а как добавление независимого источника действия в `ActionStep`:

```text
ActionStep = Tool source + PlayerAction + Target
```

Это соответствует реальному сетевому вызову Wurm:

```text
sendAction(sourceId, targetIds[], PlayerAction)
```

`Tool source` и `Target` нельзя объединять в одну сущность: первый вычисляет один source ID, второй — ноль, один или несколько target ID и стоимость очереди. Общий UI-паттерн допустим, общая модель — нет.

Рекомендуемое пользовательское название дефолтного источника — `Current active item` (pt-BR: `Item ativo atual`), а не `Anything selected`. В Wurm уже существуют active item, selected objects и hovered object. Слово `selected` у дефолтного инструмента создаёт ложное ожидание, что Keybinder возьмёт выбранный target.

Модель не разрастается бесконтрольно:

- один новый value object `ItemSelector` покрывает все способы выбора source;
- существующий `TargetSpec` остаётся только моделью цели;
- существующий `ActivateToolStep` сохраняется как расширенная команда `Switch active item`, потому что он меняет состояние HUD и полезен для старых цепочек;
- дублирование, merge и перенос используют общий deep-copy и общий portable codec;
- нормализация типа объекта выносится из `NearbyTypeTarget` в один `ObjectTypeNormalizer` и используется обеими фильтрующими целями.

## Подтверждённое текущее состояние

### Модель и хранение

- `ActionStep` хранит только `short actionId`, `TargetSpec target` и `lastKnownName`: `src/main/java/org/keybinder/wurm/model/ActionStep.java`.
- `ActivateToolStep` является отдельным `StepKind.ACTIVATE_TOOL` и хранит `TargetSpec`: `src/main/java/org/keybinder/wurm/model/ActivateToolStep.java`.
- `KeybindRecord` уже содержит стабильный ID, список вариантов и active variant: `src/main/java/org/keybinder/wurm/model/KeybindRecord.java`.
- текущая версия `KeybindStore` — 7: `src/main/java/org/keybinder/wurm/storage/KeybindStore.java:34`.
- сохранение уже использует temp file, flush/force, atomic move и rolling backup. Rolling backup не заменяет отдельную долговечную копию до schema 8.
- живой Wurm bind ссылается на стабильную команду `keybinder_run <record-id>`: `src/main/java/org/keybinder/wurm/KeybindRegistry.java:1028`. Добавление полей к record и step не требует перепривязки клавиш.
- состояние аккаунта хранит ID включённых records. Сохранение существующих record ID сохраняет account state.

### Текущее multi-поведение

- `KeybinderMod.handleKeyToggle` запускает `LongPressController`, короткое отпускание выполняет active variant, long press открывает selector: `src/main/java/org/keybinder/wurm/KeybinderMod.java:372`.
- `KeybinderMod.chooseMultiVariant` одновременно меняет active variant и выполняет его: `src/main/java/org/keybinder/wurm/KeybinderMod.java:451`.
- `KeybinderMultiSelectorWindow` всегда вызывает этот совмещённый метод: `src/main/java/com/wurmonline/client/renderer/gui/KeybinderMultiSelectorWindow.java:65`.
- mouse wheel сразу выполняет найденный record; у колеса отсутствуют press/release и long press.

### Текущий UI списка

- строки списка уже получают `clickCount` в `leftPressed(int, int, int)` и используют drag для перестановки: `src/main/java/com/wurmonline/client/renderer/gui/KeybinderWindow.java:679`.
- клиентский `WurmEventHandler` увеличивает click count при повторном нажатии той же кнопкой не позднее 500 мс. Движение мыши сбрасывает серию. Значит, double-click реализуется без собственного таймера.
- drag сейчас вычисляет только позицию вставки. Его можно расширить drop-zone режимом без второго drag-механизма.

### Текущая фильтрация nearby

- `NearbyTypeTarget` кодирует строку с префиксом `nearby ` и нормализует display name: `src/main/java/org/keybinder/wurm/command/NearbyTypeTarget.java`.
- текущий `ClientAccess.objectType` для ground/creature использует `ObjectData.getName()`: `src/main/java/org/keybinder/wurm/integration/ClientAccess.java:161`.
- `javap` подтвердил, что `ObjectData.getName()` возвращает `displayName`, а display name строится с материалом через `MaterialUtilities.appendNameWithMaterial`.
- приватное поле `ObjectData.name` содержит базовое имя, отдельное от `displayName` и `materialId`.
- `InventoryMetaItem.getBaseName()` предоставляет базовое имя без материала. Его numeric `type` нельзя использовать как единый portable type ID: у world `ObjectData` нет совместимого публичного template/type ID.

Следовательно, фильтр «без материала» должен использовать нормализованный базовый текстовый тип, а не runtime ID и не material ID.

## Источник действия в каждом ActionStep

### Подтверждённый клиентский seam

`javap` по `client-patched.jar` подтвердил:

- `HeadsUpDisplay.getSourceItemId()` существует как `protected long getSourceItemId()`;
- `HeadsUpDisplay.sendAction(PlayerAction, long[])` получает source через `getSourceItemId()`;
- затем вызывается публичный `SimpleServerConnectionClass.sendAction(long, long[], PlayerAction)`.

Надёжная реализация — signature-checked Javassist hook на `HeadsUpDisplay.getSourceItemId()`. Hook подменяет return value только при наличии scoped `ThreadLocal<Long>` override, установленного исполнителем Keybinder на время одного вызова стандартного HUD/World send path. После вызова override очищается в `finally`.

Это решение:

- сохраняет штатную Wurm-логику получения targets;
- не меняет активный предмет в HUD;
- не создаёт лишнего server action;
- позволяет target `ACTIVE_TOOL` по-прежнему означать настоящий активный предмет;
- не вмешивается в обычные действия игрока вне scoped Keybinder execution;
- совместимо с одноразовым Capture Action через существующий execution origin/depth guard.

### Точная модель

Добавить:

```text
ItemSelector
ItemSelectorKind:
  CURRENT_ACTIVE
  EMPTY_HAND
  HOVERED_ITEM
  TOOLBELT_SLOT
  EQUIPMENT_SLOT
  EXACT_OBJECT
```

Каждый `ActionStep` содержит обязательный `ItemSelector source`. Старые конструкторы `ActionStep(actionId, target)` остаются и делегируют в новый конструктор с `CURRENT_ACTIVE`, чтобы не размножать механические изменения в тестах и парсере.

Семантика:

- `CURRENT_ACTIVE`: scoped override не устанавливается; используется текущий active item Wurm.
- `EMPTY_HAND`: override `-1L`.
- `HOVERED_ITEM`: берётся ровно один hovered command target, который разрешается как `InventoryMetaItem`. World creature/ground item не принимаются как source.
- `TOOLBELT_SLOT`: сохраняется portable slot 1–10, runtime item ID разрешается перед execution.
- `EQUIPMENT_SLOT`: сохраняется portable equipment slot, runtime item ID разрешается перед execution; пустой slot означает unavailable source.
- `EXACT_OBJECT`: сохраняет существующий object/item ID и display label. Это заведомо непереносимый advanced selector; import сохраняет его, но оставляет record выключенным для проверки.

`TargetSpec` не переиспользуется для source. У source другие допустимые виды, другая кардинальность, другая валидация и нулевая queue cost.

### Почему нельзя автоматически сворачивать старый ActivateToolStep

Текущий `ExecutionPlanner` может пропустить unavailable step с нулевой стоимостью и независимо запланировать следующие steps. Значит, цепочка:

```text
Activate unavailable item
Action with current active item
```

может выполнить второй action старым активным инструментом. Автоматическое преобразование такой пары в per-action source незаметно изменит error semantics, active HUD state и повторное использование инструмента следующими steps.

Поэтому schema migration только добавляет `CURRENT_ACTIVE` к старым action steps. `ActivateToolStep` сохраняется без семантического переписывания и в UI переименовывается в `Switch active item` (pt-BR: `Trocar item ativo`), располагаясь в advanced step types.

### Пользовательский эффект

Основной сценарий становится короче и понятнее:

```text
до:  Activate hatchet -> Chop up hovered
после: Hatchet | Chop up | Hovered
```

Пользователь видит самодостаточную строку. Перестановка actions не рвёт скрытую зависимость от предыдущей активации. Повторное выполнение не оставляет другой active item в HUD.

Сложность остаётся доступной только тем, кому нужна stateful-цепочка: отдельный `Switch active item` продолжает менять активный предмет для последующих действий и обычной игры.

## Duplicate keybind

Duplicate выполняет полноценный deep copy определения, а не копирование ownership.

Точный результат:

- новый record ID;
- новые variant IDs;
- новые step instances, source selectors и target specs;
- те же action IDs, `lastKnownName`, порядок variants/steps и HUD mode;
- active variant сохраняется по прежнему индексу;
- имя `<original name> copy` через локализованную строку;
- intended key сохраняется для удобства, но record создаётся disabled с новой причиной `duplicate_review`;
- duplicate вставляется сразу после оригинала;
- creator/server штампуются текущим профилем;
- original bind backup, previous managed command, import ownership и restore metadata не копируются;
- живой bind не изменяется;
- после успешного atomic save открывается editor дубликата.

Так пользователь получает готовый шаблон для альта и явно выбирает новую клавишу, а оригинал продолжает работать.

В таблице Duplicate является текстовой кнопкой и располагается непосредственно рядом с Edit:

```text
Edit | Duplicate | Delete
Editar | Duplicar | Excluir
```

Для неё нужна собственная измеряемая localized column width; помещать Duplicate среди glyph-кнопок `+`/`−` не следует.

## Double-click edit

Использовать переданный Wurm `clickCount`.

В `SelectableRow` и row-label компонентах:

- `clickCount == 2` отменяет pending drag;
- edit откладывается через существующий UI defer mechanism ровно один раз;
- обработчик возвращается до `beginDrag`/`super.leftPressed`;
- клики по checkbox и кнопкам остаются у их собственных компонентов и не открывают editor строки.

Нужен pure input-state test, фиксирующий отсутствие одновременно edit и reorder.

## Portable import/export

### Формат

Создать отдельный `KeybindTransferStore`; не читать portable bundle напрямую через runtime `KeybindStore`.

Расширение: `.keybinder`. Формат: Java properties в UTF-8-safe существующем codec style.

Заголовок:

```properties
format=keybinder-transfer
version=1
definitionSchema=8
count=N
```

Portable definition содержит:

- name;
- intended key;
- `hudMulti`;
- variants и active variant index;
- step types;
- action IDs и `lastKnownName`;
- item selectors;
- targets и parameters;
- vanilla/raw command text.

Не экспортируются как рабочая идентичность:

- record и variant IDs;
- enabled/disabled runtime state;
- disable reason;
- creator account/server как ownership;
- original bind backup;
- previous managed command;
- per-account enabled state.

Origin account/server/version допустимы только как informational metadata и не участвуют в ownership.

### File UI

Использовать `JFileChooser` на AWT EventQueue. Parent — `org.lwjgl.opengl.Display.getParent()` (`Canvas`). Начальная папка — `mods/keybinder/transfer`, создаваемая при первом использовании. Export дописывает `.keybinder`, подтверждает замену существующего файла и пишет temp + force + atomic replace.

В UI:

- существующая кнопка `Import` получает точное имя `Import Wurm binds`;
- рядом добавляются `Import file` и `Export all`;
- export сохраняет все records в текущем порядке, включая disabled records;
- import никогда сам не включает новые records.

### Безопасный import

Лимиты:

- file size: 10 MiB;
- records: 1000;
- variants per record: 15;
- steps per variant: 100;
- record name: 80 characters;
- variant subname: 80 characters;
- vanilla/raw command: 500 characters;
- encoded selector/target field: 1024 characters.

Алгоритм:

1. Прочитать весь файл в portable DTO.
2. Проверить magic, version, counts, лимиты, enum values, action short range, обязательные parameters и все targets/sources.
3. Рассчитать semantic fingerprint, игнорируя IDs, enabled state и origin metadata.
4. Exact semantic duplicates пропустить.
5. Для остальных сгенерировать новые record/variant IDs и stamp текущего account/server.
6. Сохранить intended key, но установить disabled reason `import_review`.
7. Records с `EXACT_OBJECT` также получают `nonportable_object_review` как более конкретную причину.
8. Добавить все records одним registry transaction и одним atomic save.
9. Перечитать store и проверить новые records до публикации UI state.
10. При любой ошибке восстановить предыдущий in-memory snapshot и файл; live Wurm binds не трогать.
11. Вывести Event summary: imported, skipped duplicates, rejected.

Повторный import того же файла не размножает records благодаря semantic fingerprint. Existing records не перезаписываются, занятые клавиши не вытесняются.

## Hovered + object type filter

### Модель и названия

Добавить `TargetKind.HOVER_TYPE`, `TargetSpec.hoverType(String)` и codec prefix `hover-type `.

Пользовательские строки:

- `Hovered + object type filter` / pt-BR `Sob o cursor + filtro por tipo de objeto`;
- существующий `Nearby by type` переименовать в `Nearby + object type filter` / pt-BR `Próximo + filtro por tipo de objeto`.

Старый persisted nearby codec не переименовывается: изменение только display label, поэтому существующие records не мигрируют семантически.

### Единый тип объекта без материала

Создать `ObjectTypeNormalizer`. `NearbyTypeTarget` делегирует в него вместо собственной копии правил.

Источник базового имени:

- inventory/equipment: `InventoryMetaItem.getBaseName()`;
- ground item/creature: private `ObjectData.name` через изолированный cached reflective accessor;
- fallback: текущее display/hover name с удалением material prefix/suffix через таблицу клиента и существующие normalization rules.

Normalizer:

- lower-case с `Locale.ENGLISH`;
- схлопывает whitespace;
- убирает начальные `a`, `an`, `the`;
- убирает creature age/condition modifiers, которые уже поддерживает nearby;
- сохраняет семантический object kind;
- сохраняет специальные различия `tree stump` и `felled tree`;
- не сохраняет material.

Material ID не входит в stored type и comparison.

### Runtime hover resolution

Повторить приоритет Wurm `World.sendHoveredAction`:

1. Для action target mask с HUD bit 256 получить `hud.getCommandTargetsFrom(currentMouseX, currentMouseY)`.
2. При отсутствии HUD targets использовать `World.currentHoveredObject`, только когда он соответствует target mask.
3. Разрешить каждый target ID в inventory, ground item, creature или другой поддержанный object data.
4. Оставить только IDs, чей normalized base type равен stored filter.
5. Отправить action один раз со списком только matching IDs через штатный HUD send path и scoped source override.
6. При пустом результате ничего не отправлять и написать Event с action name, ожидаемым type и фактически найденным type/`unresolved`.

В inventory hover над выбранной строкой Wurm может вернуть несколько selected IDs. Фильтр применяется к каждому ID; mixed selection отправляет только совпавшие IDs.

Queue cost для `HOVER_TYPE` — runtime dynamic count совпавших target IDs. Preflight выполняется до первого server action всей цепочки. Нулевой count означает skipped step, а не стоимость 1.

Selection UI использует те же capture hooks, что nearby type, но отдельный `SelectionController.Mode.HOVER_TYPE`. Editor показывает сохранённый normalized type и исходный display label.

## Merge drag-and-drop

Один drag поддерживает reorder и merge:

- верхние 25% строки назначения — insertion before;
- центральные 50% — merge target;
- нижние 25% — insertion after.

Центральная зона получает отдельную Wurm-style подсветку. Drop на самого себя ничего не делает. После drop на merge target открывается confirmation window с именами обоих records.

### Точная семантика merge

Destination сохраняет:

- record ID;
- name и intended key;
- creator/server;
- ownership, backup и restore metadata;
- enabled state и disable reason;
- active variant;
- `hudMulti` mode.

Source variants deep-copy добавляются в конец destination с новыми variant IDs. Source record затем удаляется.

Имена variants:

- пустой source subname превращается в source record name;
- непустой превращается в `<source record name> — <source subname>`;
- case-insensitive collisions получают локализованный suffix ` (2)`, ` (3)` и далее.

Общий лимит 15 variants переносится из editor в shared `KeybindLimits.MAX_VARIANTS`. Merge, import, storage validation и editor используют один лимит. Превышение отклоняется до mutation.

Destination mode определяет результат: source `hudMulti` не переопределяет destination.

### Multi-to-multi и лимит

При переносе multi-keybind на другой multi-keybind объединяются оба полных списка. Destination variants сохраняют порядок и находятся первыми; source variants deep-copy добавляются следом в прежнем порядке. Active variant и `hudMulti` остаются от destination. Source active variant не становится active, а source HUD mode не меняет destination mode.

Допустимая сумма — до 15 включительно. Во время center hover UI уже знает оба counts:

- при сумме до 15 центр строки подсвечивается золотым, а confirmation показывает расчёт;
- при сумме 16 и больше центр подсвечивается красным;
- release над красной зоной показывает точную ошибку `{destination} + {source} = {result}; maximum 15` и не открывает confirmation;
- partial merge, автоматическое обрезание и автоматическое удаление лишних alternatives запрещены;
- при отказе store, оба records и оба live binds остаются без изменений.

Проверка count повторяется внутри registry transaction для защиты от изменения records между hover и confirmation.

## Подсказка Keybinds и справка

Верхняя подсказка должна занимать две строки и прямо описывать mouse gestures:

```text
EN: Double-click a row to edit it. Drag the row to reorder it.
EN: Drop it on the center of another row to merge. Use +/− to add or remove.

PT-BR: Clique duas vezes em uma linha para editá-la. Arraste a linha para reordená-la.
PT-BR: Solte-a no centro de outra linha para mesclar. Use +/− para adicionar ou remover.
```

Все новые buttons, glyphs, checkbox, selectors и action/target choices должны иметь локализованный hover text и контекстную help-строку. Для dropdown options справка выводится через обновляемый editor help label. Обязательный охват: Duplicate, import/export controls, HUD action, каждый source kind, Switch active item, оба object-type filters, merge, max-15 rejection и предупреждение о непереносимом exact object.

Поддерживаемые языки остаются `en` и `pt-BR`. Русский language enum и русский dictionary не добавляются. На момент исследования `messages_en.properties` и `messages_pt-BR.properties` содержат по 404 ключа, расхождений множеств ключей нет. Реализация обязана сохранить exact parity, включая новые help/hover/Event/error keys; placeholder sets также должны совпадать. Это проверяется automated tests, поэтому English fallback не скрывает пропущенный бразильский перевод.

### Ownership transaction

До mutation registry перечитывает source live bind и подтверждает, что command точно равна `keybinder_run <source-id>`. Destination live bind не меняется.

После подтверждения пользователя:

1. Снять in-memory и file snapshot.
2. Проверить лимиты и ownership повторно.
3. Построить merged destination и новый список records.
4. Atomic save и reload verification.
5. Удалить source live bind только при точном ownership match через Wurm parser и `saveKeyBindings()`.
6. Проверить отсутствие source command и сохранность destination command.
7. При ошибке восстановить store, records и source bind.

Imported original-binding metadata source record не переносится в destination. Confirmation явно сообщает, что merge удаляет самостоятельную restore history source, так же как delete. Автоматическое восстановление legacy bind при merge не выполняется.

## Ordinary Multi и HUD Multi

Добавить boolean `hudMulti` в `KeybindRecord`. Поле имеет смысл только при двух и более variants. При сохранении record с одним variant значение принудительно становится `false`.

Checkbox `HUD action` размещается в header editor рядом с record-level полями. Tooltip объясняет: «Открывает меню сразу; выбор выполняет действие».

### Ordinary Multi

Точная state machine:

- key press: consume, arm hold timer;
- release до threshold: выполнить active variant;
- достижение threshold: один раз открыть selector;
- release после открытия: consume, ничего не выполнять;
- выбор в selector: сохранить active variant, закрыть selector, ничего не выполнять;
- следующий короткий press выполняет новый active variant.

### HUD Multi

Точная state machine:

- первый key press: consume и немедленно открыть selector;
- key repeat: consume, не открывать второй selector;
- release: consume, ничего не выполнять;
- выбор: сохранить active variant, закрыть selector и немедленно выполнить выбранный variant;
- active variant никогда не выполняется от самого нажатия клавиши.

### Mouse wheel

У колеса нет hold gesture:

- ordinary multi: выполнить active variant;
- HUD multi: немедленно открыть selector, не выполнять active variant, pointer не перемещать.

### Реализация controller

Заменить совмещённую логику `LongPressController` на pure `MultiKeyController`, который возвращает события `EXECUTE_ACTIVE`, `OPEN_ORDINARY_SELECTOR`, `OPEN_HUD_SELECTOR`, `CONSUME`, `NONE`. Controller хранит held key, record ID, mode, press timestamp и selector-opened flag.

Selector получает явные immutable flags:

- `executeOnSelect`;
- `warpPointer`.

`chooseMultiVariant` разделяется на select-only и select-and-execute paths. Close, disconnect, HUD replacement, record disable/delete и exception очищают held controller state и singleton selector.

Execution preflight вызывается только в `EXECUTE_ACTIVE` и select-and-execute paths, не при открытии меню или ordinary selection.

## Перемещение курсора после long press

`client-patched.jar` содержит LWJGL 2 API:

- `org.lwjgl.input.Mouse.setCursorPosition(int, int)`;
- `Mouse.isCreated()`, `Mouse.isInsideWindow()`;
- `org.lwjgl.opengl.Display.getWidth()/getHeight()`.

`WurmEventHandler` переводит LWJGL Y в GUI Y как `clientHeight - mouseY`. Поэтому не нужен `java.awt.Robot`, screen coordinates или OS permission.

Порядок:

1. На первом tick selector вычисляет размер и центрируется.
2. После layout buttons на следующем tick определяется центр кнопки active variant; при отсутствии active — первой кнопки.
3. GUI coordinate переводится в LWJGL coordinate: `mouseX = uiX`, `mouseY = gameHeight - uiY`.
4. Координаты clamp в `[0, width - 1]` и `[0, height - 1]`.
5. При `Mouse.isCreated()` и `Mouse.isInsideWindow()` выполняется ровно один `setCursorPosition`.
6. Ошибка только debug-логируется и не закрывает selector.

Warp включается только для selector, открытого ordinary long press. HUD Multi и mouse wheel открывают selector без warp.

## Schema 8 и миграция действующих пользователей

Schema 8 добавляет:

- `ActionStep.source.kind` и source parameter fields;
- `KeybindRecord.hudMulti`;
- `TargetKind.HOVER_TYPE` codec support.

Правила чтения schema 1–7:

- каждому старому `ActionStep` с отсутствующим source назначается `CURRENT_ACTIVE`;
- `ActivateToolStep` остаётся stateful `Switch active item`, не удаляется и не сворачивается в source следующего action;
- отсутствующий `hudMulti` = `false`;
- nearby type codec читается без изменения stored value;
- все record IDs, variant IDs, names, keys, active variants, ordering, enabled state, disable reasons, ownership/original metadata, creator/server и `lastKnownName` сохраняются.

Перед первой записью schema 8 после успешной загрузки старой схемы создать один долговечный `keybinds.pre-v8.properties` из реально загруженного source file. Не перезаписывать существующую pre-v8 копию. Затем сохранить schema 8, перечитать и сравнить IDs/count/semantic definitions. Только после проверки заменить registry state.

Обычный rolling `.bak` продолжает работать для повреждения последней записи. `pre-v8` предназначен для ручного downgrade и не ротируется.

Миграция не меняет live Wurm commands, потому что они привязаны к record ID. Она не включает disabled records и не занимает новые клавиши.

## Риски и закрывающие меры

| Риск | Решение |
|---|---|
| Source hook влияет на обычные actions | ThreadLocal override только внутри Keybinder, обязательный `finally`, fail-open для vanilla |
| Explicit source не разрешился | Полный preflight; цепочка не начинает отправку; точная Event reason |
| Старая Activate-цепочка меняет смысл | Не преобразовывать автоматически |
| Duplicate вытесняет оригинал | Создавать disabled, live bind не трогать |
| Повторный file import плодит копии | Semantic fingerprint и skip exact duplicate |
| Import переносит чужие ownership IDs | Генерировать все runtime IDs заново, ownership metadata исключить |
| Object type зависит от материала | Брать base/raw name, единый normalizer, material исключить |
| Merge теряет source при частичной ошибке | Один registry transaction, ownership preflight, reload verification, rollback |
| Double-click запускает drag | Обработать `clickCount == 2` до drag |
| HUD key repeat открывает окна | Controller хранит opened flag и singleton selector |
| Cursor warp ломается в fullscreen | LWJGL client coordinates, one-shot, fail-open |
| Schema 8 нельзя открыть старой версией | Одноразовая `pre-v8` копия и честный schema number 8 |

## Обязательная проверка реализации

Unit/integration tests должны покрыть:

- schema 1–7 -> 8 с сохранением IDs, ownership и semantics;
- создание и однократность pre-v8 backup;
- source codec/validation/resolution для всех `ItemSelectorKind`;
- scoped source override cleanup после success и exception;
- fail-open vanilla action и fail-closed explicit-source Keybinder action;
- deep duplicate без разделяемых mutable objects и ownership metadata;
- portable export/import roundtrip, malformed/oversized input, exact duplicate skip и rollback;
- object normalization без material для inventory, ground items, creatures, stump/felled tree;
- mixed HUD selected targets и dynamic cost для `HOVER_TYPE`;
- drag zone classification, reorder, confirmed merge, cancel, max limit, ownership conflict и rollback;
- ordinary/HUD multi press/repeat/release/threshold/select state machines;
- wheel semantics;
- double-click edit без reorder;
- one-shot cursor coordinate conversion/clamping;
- localization invariants для всех новых visible strings.

Manual matrix:

- migration копии реального schema 7 profile;
- duplicate сложного multi для второго персонажа;
- export из одной Wurm installation и import в другую;
- hover filter на одинаковых объектах разных материалов;
- mixed selected inventory objects;
- merge enabled/disabled/imported records и отмена confirmation;
- ordinary multi, HUD multi, key repeat и wheel;
- cursor warp windowed/fullscreen/multi-monitor;
- reconnect/HUD recreation без duplicate selector и held state;
- source selection toolbelt/equipment/hover/exact;
- Wurm HUD fade/hover/resize на всех новых controls.

Финальная автоматическая команда остаётся:

```powershell
$env:JAVA_HOME='C:\projects\tools\temurin8\jdk8u492-b09'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:GRADLE_USER_HOME='C:\projects\wurm\.gradle'
.\gradlew.bat clean build dist --stacktrace
```

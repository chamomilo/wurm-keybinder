# Prompt: реализовать следующий релиз Keybinder

Работай в репозитории `C:\projects\wurm`. Реализуй весь объём этой задачи до зелёного `clean build dist`. Не ограничивайся анализом, планом или частичной заготовкой.

## Обязательная база

Сначала полностью прочитай:

1. `AGENTS.md`;
2. `RESEARCH_REPORT.md`;
3. `KEYBINDER_NEXT_RELEASE_RESEARCH_REPORT.md`;
4. затрагиваемые production tests и production classes.

Считай `KEYBINDER_NEXT_RELEASE_RESEARCH_REPORT.md` закрытым техническим решением. Не оставляй архитектурных развилок, TODO, временных adapters и неподключённых UI controls. Сохрани namespace `org.keybinder.wurm`, standalone artifact и все правила ownership, transaction, queue preflight, fail-open hooks, Wurm GUI opacity и attribution из `AGENTS.md`.

Не изменяй пользовательские файлы `SKLOTOPOLIS_ANNOUNCEMENT*`. Не стирай посторонние изменения рабочего дерева.

## Результат релиза

Релиз одновременно добавляет:

1. независимый `Tool source` в каждый custom action: `Tool | Action | Target`;
2. duplicate keybind;
3. edit keybind по double-click;
4. portable import/export `.keybinder`;
5. target `Hovered + object type filter` и новое display name `Nearby + object type filter`;
6. drag-to-merge records с confirmation;
7. отдельный режим `HUD Multi`;
8. one-shot перенос курсора к active choice после ordinary long press;
9. schema 8 с безопасной миграцией существующей базы;
10. полная контекстная справка и hover text для каждого нового интерактивного элемента и режима действия;
11. полная локализация всех новых строк на существующие языки English и Português (Brasil).

## 1. Зафиксируй baseline

До production-изменений запусти текущие tests на JDK 8. Зафиксируй падающие baseline tests в рабочем отчёте. После этого добавляй изменения небольшими связными блоками и запускай релевантные tests после каждого блока.

## 2. Общие limits и deep-copy

Создай `org.keybinder.wurm.model.KeybindLimits`:

```text
MAX_VARIANTS = 15
MAX_STEPS_PER_VARIANT = 100
MAX_RECORDS_IN_TRANSFER = 1000
MAX_RECORD_NAME_LENGTH = 80
MAX_VARIANT_NAME_LENGTH = 80
MAX_COMMAND_LENGTH = 500
MAX_ENCODED_FIELD_LENGTH = 1024
MAX_TRANSFER_FILE_BYTES = 10 * 1024 * 1024
```

Удали локальный `MAX_VARIANTS` из `KeybinderEditorWindow`. Подключи shared limits в editor, validator, merge, transfer import и storage validation.

Создай один production deep-copy service для record definitions. Он копирует variants, mixed steps, action source, target, action ID, `lastKnownName`, command flags и active variant по индексу. Он всегда создаёт новые record/variant IDs для duplicate, merge-source variants и file import. Не разделяй mutable lists или step objects между оригиналом и копией.

## 3. Добавь ItemSelector в ActionStep

Создай immutable classes:

```text
org.keybinder.wurm.model.ItemSelector
org.keybinder.wurm.model.ItemSelectorKind
```

Enum содержит ровно:

```text
CURRENT_ACTIVE
EMPTY_HAND
HOVERED_ITEM
TOOLBELT_SLOT
EQUIPMENT_SLOT
EXACT_OBJECT
```

`ItemSelector` хранит `kind`, `slot`, `objectId`, `text`, предоставляет validating factories, getters, `equals` и `hashCode`.

Правила factories:

- `currentActive()` не содержит parameter;
- `emptyHand()` не содержит parameter;
- `hoveredItem()` не содержит parameter;
- `toolbeltSlot(int)` принимает 1–10;
- `equipmentSlot(int)` принимает диапазон client equipment slot;
- `exactObject(long, String)` сохраняет ID и display label.

Добавь обязательное поле `ItemSelector source` в `ActionStep`. Сохрани старые constructors и направь их в новый constructor с `ItemSelector.currentActive()`. Добавь новый constructor `(short actionId, ItemSelector source, TargetSpec target, String lastKnownName)`.

Не меняй `TargetSpec` на универсальный selector. Не помещай source kinds в `TargetKind`.

Сохрани `ActivateToolStep` как отдельный stateful advanced step. В English UI назови его `Switch active item`, в pt-BR — `Trocar item ativo`. Не преобразовывай старые Activate steps в action source.

Legacy `act` parser, vanilla import и одноразовый `Capture Action` создают action steps с `CURRENT_ACTIVE`. Capture исключает Keybinder execution и не сохраняет runtime active item ID.

## 4. Добавь source codec, validation и schema 8

Подними `KeybindStore.SCHEMA_VERSION` с 7 до 8.

Для каждого `ActionStep` schema 8 записывает source fields рядом с action fields:

```text
sourceKind
sourceSlot
sourceObjectId
sourceText
```

При миграции schema 1–7 назначай каждому старому `ActionStep` с отсутствующим source значение `CURRENT_ACTIVE`. Существующие `ActivateToolStep` сохраняй как stateful `Switch active item`: не удаляй их и не сворачивай в source следующего action. Так активация продолжает влиять на все последующие steps и оставляет выбранный предмет активным после завершения цепочки.

Добавь boolean `hudMulti` в `KeybindRecord`, getter/setter и сохранение `record.<n>.hudMulti`. Constructors устанавливают `false`. Schema 1–7 читает `false`. Setter и save normalization принудительно устанавливают `false` у record с одним variant.

Проверь все места, которые строят replacement record или копируют record fields. Они обязаны сохранять `hudMulti`, source selectors, active variant, enabled state, disable reason, ownership, original bind metadata и creator metadata согласно назначению операции.

Перед первой schema 8 записью базы, успешно прочитанной как schema 1–7, создай рядом один файл `keybinds.pre-v8.properties`:

- копируй фактически загруженный main source, либо восстановленный `.bak`, когда load был выполнен из backup;
- используй create-new semantics;
- не перезаписывай существующий pre-v8 файл;
- force содержимое на диск;
- затем выполни обычный atomic schema 8 save;
- перечитай schema 8 и сравни record IDs, variant IDs, порядок и semantic definitions;
- публикуй migrated registry state только после успешной проверки.

Сохрани rolling `.bak` отдельно от pre-v8 backup.

Добавь tests для schema 1–7 fixtures, schema 8 roundtrip, corrupted main + backup migration, однократности pre-v8 copy и сохранения всех IDs/ownership fields.

## 5. Внедри scoped source override в клиент

Создай `org.keybinder.wurm.integration.ActionSourceOverride` с `ThreadLocal<Long>` и scoped API. Scoped API:

- устанавливает override только на время одного штатного Wurm send path;
- восстанавливает предыдущее nested value;
- очищает ThreadLocal в `finally`;
- не протекает после exception.

Добавь signature-checked Javassist hook:

```text
class: com.wurmonline.client.renderer.gui.HeadsUpDisplay
method: getSourceItemId
descriptor: ()J
```

After original method передай `$_` через `ActionSourceOverride.overrideOr($_)`. Изолируй hook рядом с другими integration hooks, логируй signature mismatch и сохрани fail-open поведение обычного клиента.

Запомни capability status hook. `CURRENT_ACTIVE` работает через старый path независимо от capability. Explicit source требует capability; отсутствие capability блокирует Keybinder chain до первого send и показывает точную Event/error reason.

Добавь source resolver в integration/execution layer:

- `CURRENT_ACTIVE`: не устанавливать override;
- `EMPTY_HAND`: source ID `-1L`;
- `HOVERED_ITEM`: получить текущие HUD command targets и принять ровно один ID, разрешённый как `InventoryMetaItem`; world creature и ground item не принимаются;
- `TOOLBELT_SLOT`: разрешить сохранённый slot 1–10 в текущий runtime item ID;
- `EQUIPMENT_SLOT`: разрешить slot в текущий runtime item ID; пустой slot unavailable;
- `EXACT_OBJECT`: разрешить сохранённый ID только как доступный inventory item.

Добавь source resolution в полный chain preflight до первого server action. Source не увеличивает queue cost. Unavailable explicit source блокирует всю action chain и сообщает record, variant, step, selector и причину. Не допускай частичного execution.

Во время actual action execution используй существующий штатный HUD/World target send path внутри scoped source override. Не вызывай `setActiveToolAt` и не меняй HUD active item для per-action source.

Сохрани execution origin/depth guard, чтобы одноразовый `Capture Action` не принял Keybinder send за действие пользователя.

Добавь tests для всех source kinds, nested override, cleanup, exception, absent capability, no partial send и неизменности current HUD active item.

## 6. Перестрой action editor в Tool | Action | Target

В каждой строке custom action editor показывай три логические колонки:

```text
Tool | Action | Target
```

Default Tool label:

```text
en:    Current active item
pt-BR: Item ativo atual
```

Не используй visible label `Anything selected`.

Tool selector содержит:

- Current active item;
- Empty hand;
- Hovered inventory item;
- Toolbelt slot;
- Equipment slot;
- Exact inventory item.

Переиспользуй one-shot toolbelt/equipment/exact inventory capture infrastructure через отдельный source editing callback. Не сохраняй runtime ID для portable slots. Exact inventory item остаётся advanced непереносимым вариантом и показывает предупреждение.

Показывай concise source summary в списке steps и record summary. Добавь field-level validation: missing selector parameter, invalid slot, unavailable editor selection и malformed exact ID блокируют save.

Оставь `Switch active item` в меню добавления advanced steps. Его UI продолжает редактировать старый `TargetSpec` и сохраняет stateful semantics.

## 7. Введи единый ObjectTypeNormalizer

Создай `org.keybinder.wurm.command.ObjectTypeNormalizer`. Перенеси в него rules из `NearbyTypeTarget.normalizeType`. `NearbyTypeTarget` делегирует ему encode/matches.

Normalizer:

- использует `Locale.ENGLISH` lower-case;
- trim и collapse whitespace;
- удаляет начальные `a`, `an`, `the`;
- удаляет поддержанные creature age/condition modifiers;
- удаляет material component;
- сохраняет object kind;
- сохраняет отдельные типы `tree stump` и `felled tree`.

Расширь `ClientAccess` cached reflective accessor к private `ObjectData.name`. Для world ground items/creatures сначала используй raw `ObjectData.name`; для inventory/equipment используй `InventoryMetaItem.getBaseName()`. Текущий display/hover name служит fallback и проходит material stripping. Reflection integration signature-check, log и fail-open.

Не используй `InventoryMetaItem.getType()` как portable filter: world objects не предоставляют совместимый единый ID.

Добавь parameterized tests с одинаковыми object types разных materials, articles, creature modifiers, stump/felled tree, inventory base names и world raw names.

## 8. Добавь TargetKind.HOVER_TYPE

Добавь:

```text
TargetKind.HOVER_TYPE
TargetSpec.hoverType(String)
TargetCodec prefix: hover-type 
SelectionController.Mode.HOVER_TYPE
```

Visible labels:

```text
en:    Hovered + object type filter
pt-BR: Sob o cursor + filtro por tipo de objeto

en:    Nearby + object type filter
pt-BR: Próximo + filtro por tipo de objeto
```

Существующий nearby stored codec и enum сохраняются. Переименуй только visible nearby label.

Editor capture для `HOVER_TYPE` использует те же inventory/world hooks, что nearby type, но отдельный mode и callback. Храни normalized base type и исходный display label для понятного summary.

Runtime resolver повторяет Wurm hover priority:

1. Для PlayerAction target mask с HUD bit 256 получить `HeadsUpDisplay.getCommandTargetsFrom(currentMouseX, currentMouseY)` через package bridge/integration accessor.
2. При пустом HUD результате получить `World.currentHoveredObject` и проверить action target mask.
3. Разрешить каждый ID через inventory, ground item, creature и поддержанный object data.
4. Нормализовать base type каждого объекта.
5. Оставить только совпавшие IDs.
6. Отправить один action с массивом совпавших IDs через штатный HUD path и scoped source override.
7. При нуле совпадений не отправлять server action и написать `[Keybinder]` Event с action catalog name, expected type и found type либо `unresolved`.

Inventory selected group может вернуть несколько IDs. Фильтруй каждый ID независимо и отправляй только совпавшие.

Queue classifier для `HOVER_TYPE` — `DYNAMIC`. Runtime cost равна количеству matching IDs. Включи это разрешение в общий full-chain preflight. Cost zero означает skipped action step. Превышение лимита блокирует цепочку до первого send.

Добавь tests для codec, editor validation, HUD/world priority, mixed selected group, unresolved IDs, zero match, dynamic queue count и no partial execution.

## 9. Раздели Ordinary Multi и HUD Multi

Размести checkbox `HUD action` в header `KeybinderEditorWindow` среди record-level settings. Checkbox активен только при двух и более variants. При удалении variants до одного сразу снять checkbox и сохранить `hudMulti=false`.

Tooltip и help text точно описывают поведение:

```text
en:    Open the choice menu immediately; choosing an item runs it.
pt-BR: Abre o menu de opções imediatamente; escolher um item o executa.
```

Замени `LongPressController` на pure `MultiKeyController`. Он не обращается к Wurm classes и возвращает events:

```text
EXECUTE_ACTIVE
OPEN_ORDINARY_SELECTOR
OPEN_HUD_SELECTOR
CONSUME
NONE
```

Он хранит held key, record ID, mode, press time и selector-opened flag.

Ordinary Multi state machine:

- press consumes и armed timer;
- release до threshold возвращает `EXECUTE_ACTIVE`;
- threshold один раз возвращает `OPEN_ORDINARY_SELECTOR`;
- release после threshold возвращает `CONSUME`;
- selector choice сохраняет active variant и закрывает окно без execution;
- следующий короткий press выполняет новый active variant.

HUD Multi state machine:

- первый press возвращает `OPEN_HUD_SELECTOR`;
- repeated press возвращает `CONSUME`;
- release возвращает `CONSUME`;
- selector choice сохраняет active variant, закрывает окно и выполняет выбранный variant;
- сам press никогда не выполняет active variant.

Обычный single-variant record сохраняет старое прямое execution.

Mouse wheel:

- ordinary multi выполняет active variant;
- HUD multi открывает selector немедленно, не выполняет active variant и не перемещает pointer;
- single variant выполняется напрямую.

Передай в `KeybinderMultiSelectorWindow` immutable selection mode с двумя flags:

```text
executeOnSelect
warpPointer
```

Раздели текущий `chooseMultiVariant` на два controller paths: select-only и select-and-execute. Persist active variant до execution. Execution failure не откатывает пользовательский выбор.

Close, disconnect, HUD replacement, disable/delete текущего record и exception очищают held state и закрывают singleton selector. Не создавай duplicate selector на key repeat.

Queue preflight и execution logging запускаются только при фактическом action execution, не при открытии selector и не при ordinary selection.

Добавь exhaustive pure tests для press/repeat/release/threshold/select обеих state machines, close/reset, disabled record, mouse wheel и single variant.

## 10. Перемести pointer после ordinary long press

Используй LWJGL 2:

```text
org.lwjgl.input.Mouse.setCursorPosition(int, int)
Mouse.isCreated()
Mouse.isInsideWindow()
org.lwjgl.opengl.Display.getWidth()
Display.getHeight()
```

Не используй `java.awt.Robot`.

`KeybinderMultiSelectorWindow` выполняет one-shot warp только при `warpPointer=true`:

1. первый tick рассчитывает размер и центрирует окно;
2. следующий tick после layout вычисляет центр button active variant, либо первой button при отсутствии active match;
3. переводит GUI Y в LWJGL Y формулой `gameHeight - uiY`;
4. clamp X/Y в client bounds;
5. при созданной mouse и pointer внутри client вызывает `setCursorPosition` ровно один раз;
6. помечает попытку завершённой;
7. runtime exception пишет только debug log и оставляет selector рабочим.

HUD Multi, mouse wheel и programmatic selector open всегда передают `warpPointer=false`.

Вынеси coordinate conversion и clamp в pure helper, добавь tests для center, границ и Y inversion.

## 11. Добавь Duplicate рядом с Edit

В managed keybind list добавь полноценную текстовую `WButton`, а не glyph. Порядок трёх буквенных кнопок в каждой строке строго такой:

```text
Edit | Duplicate | Delete
Editar | Duplicar | Excluir
```

`Duplicate` находится непосредственно после `Edit` и перед `Delete`. Добавь отдельную width column `requiredDuplicateWidth`, рассчитываемую через `localizedButtonColumnWidth`, и учти её в minimum width, available width, table header, `TableRow` и layout. Расширь `KeybinderUiController` и `KeybindRegistry` отдельной операцией duplicate.

Операция:

1. перечитывает source record по ID;
2. deep-copy definition с новым record ID и новыми variant IDs;
3. сохраняет active variant по индексу;
4. сохраняет source selectors, targets, steps, `lastKnownName`, variant order и `hudMulti`;
5. задаёт локализованное имя `<name> copy`;
6. сохраняет intended key;
7. устанавливает `enabled=false` и disable reason `duplicate_review`;
8. stamp current user/server;
9. очищает `originalKey`, `originalCommand`, `previousManagedCommand` и import/restore ownership;
10. вставляет duplicate сразу после source;
11. выполняет один atomic save и reload verification;
12. не меняет live Wurm binds;
13. refresh списка и открывает editor нового record.

Добавь exact localized reason: дубликат требует выбрать свободную клавишу и проверить настройки.

Добавь tests для mixed steps, multi variants, active index, deep-copy independence, metadata clearing, insertion order, save failure rollback, отсутствия bind mutation, порядка `Edit | Duplicate | Delete` и измеренной ширины `Duplicate`/`Duplicar`.

## 12. Добавь edit по double-click

В `SelectableRow` и row-owned labels обработай `clickCount == 2` до drag initialization:

- cancel pending drag state;
- defer ровно один вызов edit по record ID;
- return без `beginDrag` и без reorder;
- checkbox, Duplicate, Edit и Delete сохраняют собственную обработку и не bubble double-click в row.

Сохрани single-click drag/reorder. Добавь pure interaction tests: single press drag, double-click edit, отсутствие одновременного edit/reorder, double-click на button.

Перепиши верхнюю подсказку окна `Keybinds` в две короткие строки, чтобы она объясняла все mouse gestures и не раздувала minimum width:

```properties
# messages_en.properties
list.instructions=Double-click a row to edit it. Drag the row to reorder it.
list.instructions.merge=Drop it on the center of another row to merge. Use +/− to add or remove.

# messages_pt-BR.properties
list.instructions=Clique duas vezes em uma linha para editá-la. Arraste a linha para reordená-la.
list.instructions.merge=Solte-a no centro de outra linha para mesclar. Use +/− para adicionar ou remover.
```

`KeybinderWindow` показывает обе строки отдельными `WurmLabel` над filters.

## 13. Расширь drag-and-drop режимом merge

Расширь существующий row drag, не создавай параллельную DnD систему.

Drop zones строки назначения:

- top 25%: reorder before;
- center 50%: merge;
- bottom 25%: reorder after.

Вынеси classification в pure helper. Center zone рисует отдельный Wurm-style merge highlight с constant alpha `1.0f`. Для допустимого merge используй золотую подсветку; для суммы свыше 15 — красную. Drop на source row no-op. Filtered list передаёт visible IDs в reorder, merge принимает реальные source/destination IDs.

При center hover заранее вычисли `destination.variantCount + source.variantCount`.

- Сумма 15 и меньше открывает после drop один confirmation window.
- Confirmation показывает оба имени, расчёт `{destination count} + {source count} = {result count}`, сохранение destination key/HUD mode и удаление самостоятельного source record с его restore history.
- Сумма 16 и больше не открывает confirmation. После drop покажи blocking error с обоими именами и точным расчётом: `Cannot merge: {destination count} + {source count} = {result count} variants; maximum 15. Remove alternatives first.`
- При превышении не копируй часть variants, не удаляй source, не сохраняй store и не меняй ни один live bind.
- Cancel допустимого merge не меняет store и binds.

Multi-to-multi всегда объединяет полные наборы: исходные destination variants остаются первыми в прежнем порядке, затем добавляются deep-copied source variants в прежнем порядке. Destination active variant остаётся прежним. Destination `hudMulti` mode остаётся прежним; source mode отбрасывается вместе с source record. Результат остаётся multi-keybind.

Централизованная merge operation в `KeybindRegistry`:

1. запрещает self-merge;
2. перечитывает оба records;
3. проверяет `destination count + source count` против `KeybindLimits.MAX_VARIANTS` до confirmation и повторно внутри transaction;
4. перечитывает live source bind;
5. разрешает удаление bind только при exact normalized key и command `keybinder_run <source-id>`;
6. сохраняет destination ID, name, key, creator/server, ownership/original metadata, enabled state, disable reason, active variant и `hudMulti`;
7. deep-copy source variants с новыми IDs и append в destination;
8. для пустого source subname использует source record name;
9. для непустого использует `<source record name> — <source subname>`;
10. case-insensitive name collision получает suffix ` (2)`, ` (3)`;
11. удаляет source record из model list;
12. не переносит source original/restore metadata в destination;
13. atomic save + reload verification;
14. conditionally удаляет exact-owned source bind через `WurmConsole.handleInput(...)` и `saveKeyBindings()`;
15. проверяет отсутствие source command и сохранность destination command;
16. при ошибке восстанавливает file, in-memory records и source live bind.

Не восстанавливай legacy original bind source автоматически. Не изменяй destination live bind.

Добавь tests для zone boundaries, multi-to-multi order, destination active variant, destination HUD mode, сумм 14/15/16, красной invalid highlight, отсутствия confirmation/mutation при 16+, confirmation cancel, name derivation/collisions, enabled/disabled source, ownership mismatch, atomic failure и live-bind rollback.

## 14. Добавь portable `.keybinder` transfer

Создай отдельный package `org.keybinder.wurm.transfer` с DTO, semantic fingerprint, codec/store и import service. Не загружай transfer file через runtime `KeybindStore`.

Формат Java properties:

```properties
format=keybinder-transfer
version=1
definitionSchema=8
count=N
```

Строковые значения кодируй Base64 UTF-8 так же, как `KeybindStore`. Файл имеет extension `.keybinder`.

Экспортируй в текущем порядке все records, включая disabled:

- name;
- intended key;
- `hudMulti`;
- variants;
- active variant index, не runtime variant ID;
- all mixed steps;
- action ID, `lastKnownName`, source и target;
- vanilla/raw command и exact-text flag.

Не экспортируй runtime identity/ownership как восстанавливаемые поля:

- record IDs;
- variant IDs;
- enabled state и disable reason;
- original key/command;
- previous managed command;
- account enabled state.

Origin user/server, Keybinder version и export timestamp запиши только как informational header. Import не переносит их в ownership.

Создай semantic fingerprint из canonical portable definition без IDs, enabled state и origin metadata. Fingerprint включает name, intended key, hud mode, active index, variant names/order, step order, source, action ID, target и commands.

Import limits использует `KeybindLimits`. До model mutation:

1. отклонить file больше 10 MiB;
2. полностью parse properties;
3. проверить magic/version/schema/counts;
4. проверить field length, enum, numeric range, source/target parameters и mixed step rules;
5. построить все DTO;
6. посчитать fingerprints;
7. пропустить definitions, fingerprint которых уже есть в registry или ранее в этом файле.

Для каждого нового definition:

- сгенерировать record и variant IDs;
- stamp current account/server;
- сохранить name, intended key, `hudMulti`, active index и steps;
- установить disabled reason `import_review`;
- record с `EXACT_OBJECT` получает более конкретный reason `nonportable_object_review`;
- не устанавливать live bind.

Добавь все imported records одним registry transaction и одним atomic save. Перечитай store и проверь imported definitions. Любая ошибка возвращает предыдущие file и in-memory snapshots. Live Wurm binds остаются неизменными.

Покажи Event summary: imported count, skipped exact duplicates, rejected count и path.

File chooser integration:

- `JFileChooser` запускается на AWT EventQueue;
- owner — `org.lwjgl.opengl.Display.getParent()` Canvas; null parent передаётся как null owner;
- default directory — `mods/keybinder/transfer`;
- import filter показывает `.keybinder`;
- export добавляет extension;
- существующий export target требует overwrite confirmation;
- export пишет unique temp, flush, `FileChannel.force(true)`, atomic replace с supported fallback;
- callback в Wurm UI выполняется через существующий defer mechanism;
- chooser никогда не блокирует render thread.

В верхних controls `Keybinds`:

- переименуй существующий `Import` в `Import Wurm binds`;
- добавь `Import file`;
- добавь `Export all`.

Добавь tests для roundtrip всех step/source/target kinds, Unicode, deterministic fingerprint, repeated import, malformed Base64, unknown enum/version, all limits, duplicate within file, exact-object review, save rollback и no bind mutation.

## 15. Registry, ownership и UI synchronization

Все новые mutations проходят через `KeybindRegistry`, а не напрямую из Wurm GUI classes. Расширь controller interfaces минимальными typed methods для duplicate, confirmed merge, transfer callbacks и HUD mode.

Для duplicate/import не снимай и не устанавливай live binds. Для merge меняй только exact-owned source bind после successful model save и с rollback. Для schema migration live binds не меняй: dispatcher command остаётся `keybinder_run <record-id>`.

После каждой successful mutation:

- refresh main list;
- синхронизируй открытый editor;
- закрой editor удалённого source record после merge;
- закрой selector удалённого/disabled record;
- Event log содержит одну понятную локализованную запись;
- debug details идут в mod log.

Не перезаписывай foreign bind без существующего confirmation workflow.

## 16. Локализация и пользовательские тексты

Поддерживаемые языки остаются ровно такими, как в текущем коде:

```text
ENGLISH (en)
PORTUGUESE_BRAZIL (pt-BR)
```

Не добавляй русский язык, `messages_ru.properties` или значение Russian в `Language`. Каждую новую visible, help, hover, confirmation, validation, reason и Event строку добавь одновременно в `messages_en.properties` и `messages_pt-BR.properties`. Не полагайся на English fallback для новых ключей. Используй естественный бразильский португальский, а не европейский вариант.

Расширь localization tests:

- exact equality множеств ключей English и pt-BR dictionaries;
- ни одно значение не пустое;
- одинаковые MessageFormat placeholders `{0}`, `{1}` и далее в обеих версиях каждого ключа;
- выбор pt-BR не возвращает English fallback ни для одного нового ключа;
- machine values, IDs, codecs и dispatcher commands не зависят от языка.

Не оставляй visible hard-coded text.

Обязательные понятия:

- Tool / Ferramenta;
- Current active item / Item ativo atual;
- Empty hand / Mão vazia;
- Hovered inventory item / Item do inventário sob o cursor;
- Switch active item / Trocar item ativo;
- Duplicate / Duplicar;
- duplicate review reason;
- Import Wurm binds / Importar atalhos do Wurm;
- Import file / Importar arquivo;
- Export all / Exportar tudo;
- import review reason;
- nonportable object review reason;
- Hovered + object type filter / Sob o cursor + filtro por tipo de objeto;
- Nearby + object type filter / Próximo + filtro por tipo de objeto;
- HUD action / Ação da HUD;
- ordinary/HUD multi help;
- merge confirmation, source/destination, limit и ownership errors;
- export/import summaries;
- source unavailable/capability errors;
- hover expected/found mismatch.

## 17. Hover text и контекстная справка

Каждый новый интерактивный control получает непустой локализованный `setHoverString(...)`. Это касается:

- `Duplicate` в каждой строке;
- `Import Wurm binds`, `Import file`, `Export all`;
- checkbox `HUD action`;
- Tool dropdown;
- всех source capture/select buttons;
- target `Hovered + object type filter` и его capture button;
- merge-capable row/center drop zone;
- confirmation controls merge/import/export overwrite;
- новых glyphs, checkbox и кнопок, добавленных в ходе реализации.

Tooltip сообщает результат нажатия и важный side effect. `Duplicate` говорит, что будет создана disabled copy для новой клавиши. `HUD action` объясняет immediate menu и execute-on-selection. `Import file` говорит, что imported records останутся disabled до review. `Export all` говорит, что экспортируются все records. Merge row tooltip перечисляет double-click, reorder и center-drop merge; invalid merge text сообщает рассчитанное превышение 15.

Для options, у которых Wurm dropdown не поддерживает отдельный tooltip на каждый пункт, добавь существующий editor help label и обновляй его при смене выбранного option. Добавь отдельные help keys для:

- каждого `ItemSelectorKind`;
- `Switch active item`;
- `Hovered + object type filter`;
- `Nearby + object type filter`;
- Ordinary Multi;
- HUD Multi;
- duplicate review;
- merge semantics и max-15 rejection;
- portable exact-object warning.

Не меняй размер controls на каждом tick. Обновляй help/hover text только при фактической смене выбранного значения. Все help и hover keys присутствуют в en и pt-BR и входят в localization parity/placeholder tests.

Создай testable `UiHelpContract` или эквивалентный registry обязательных message keys для новых controls и modes. Test проверяет, что каждому перечисленному control/mode соответствует непустой English и pt-BR hover/help key. GUI smoke tests проверяют вызов tooltip binding для новых buttons и checkbox.

## 18. Обнови документацию

Обнови `readme.md`:

- объясни `Tool | Action | Target`;
- различи per-action Tool и advanced `Switch active item`;
- опиши duplicate и double-click;
- опиши `.keybinder` transfer и то, что imported records требуют review;
- опиши оба object type filters без material;
- опиши drag-to-merge и потерю самостоятельной source restore history;
- сравни Ordinary Multi и HUD Multi;
- упомяни pointer warp после long press;
- опиши schema 8 pre-v8 backup и downgrade через ручное восстановление.

Сохрани legacy migration contract и attribution.

## 19. Tests и финальная проверка

Добавь и обнови automated tests из каждого раздела. Особо проверь:

- старые legacy parsing/generation tests без изменения semantics;
- existing ActivateToolStep chains;
- storage migration fixtures;
- queue cost/no partial execution;
- bind ownership/rollback;
- one-shot Capture Action recursion/deduplication;
- HUD lifecycle и singleton selectors;
- custom rendering constant alpha и отсутствие tick-time resize.

Запусти JDK 8 командой:

```powershell
$env:JAVA_HOME='C:\projects\tools\temurin8\jdk8u492-b09'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:GRADLE_USER_HOME='C:\projects\wurm\.gradle'
.\gradlew.bat clean build dist --stacktrace
```

После build проверь содержимое JAR/ZIP:

- main class `org.keybinder.wurm.KeybinderMod`;
- только новый implementation namespace;
- один standalone Keybinder mod;
- license и attribution присутствуют;
- transfer classes и новые resources упакованы.

Ручные проверки не объявляй выполненными без запуска Wurm. В финальном отчёте раздели:

- реализовано;
- automated verification с точной командой и результатом;
- manual Wurm matrix, оставшаяся пользователю;
- migration/backup location;
- затронутые файлы;
- известные ограничения: exact object source непереносим, cursor warp fail-open, imported/duplicated records disabled до review.

Не объявляй задачу завершённой до успешного `clean build dist` и всех applicable automated tests.

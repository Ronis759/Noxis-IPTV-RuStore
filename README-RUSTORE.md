# Noxis IPTV — версия для RuStore

Отдельный проект: та же программа, но подготовленная под правила RuStore.
Основной проект (сборки с GitHub): `E:\OpenCode\Noxis-IPTV`.

## Чем отличается от обычной версии

| | Обычная (GitHub) | RuStore |
|---|---|---|
| `applicationId` | `app.iptv.tv` | `app.iptv.tv.rustore` |
| Название на ТВ | Noxis IPTV | Noxis IPTV (то же; отличается только иконка) |
| Иконка | «N» + цветная полоска | то же + зелёная метка RuStore в углу |
| Разрешение `REQUEST_INSTALL_PACKAGES` | есть | **нет** |
| Разрешение `WRITE_EXTERNAL_STORAGE` | есть (Android 6–9) | **нет** |
| Автообновление APK | есть | **нет** |
| Кнопка в настройках | «Проверить обновления» | «Обновите через RuStore» |
| Версия | 2.9.x (код = номер сборки GitHub) | 3.2.4 (код 35) |

Почему убрано автообновление: модерация RuStore (п. 2.14) не пропускает разрешение
`REQUEST_INSTALL_PACKAGES` — оно позволяет устанавливать APK в обход магазина.
Разрешение нужно было только встроенному автообновлению, поэтому оно удалено целиком:

- `AndroidManifest.xml` — нет `<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />`;
- нет `FileProvider` (он использовался только для запуска установщика APK);
- удалены `Updater.kt` (скачивание и запуск установщика) и `res/xml/file_paths.xml`;
- `UpdateFlow.kt` заменён на пустую версию: проверок нет, вместо установки — подсказка
  «Обновите приложение через RuStore»;
- `BuildConfig.UPDATES_ENABLED = false`.

Почему убрано разрешение `WRITE_EXTERNAL_STORAGE`: RuStore считает его чувствительным и требует
объяснение. Оно было нужно только для записи копии настроек в общую папку «Загрузки» на Android 6–9.
Теперь разрешения нет:

- на Android 10+ копия пишется в «Загрузки» через `MediaStore` (разрешение не требуется, как и раньше);
- на Android 6–9 копия сохраняется в папку приложения (`Android/data/app.iptv.tv.rustore/files/`)
  или через «Сохранить как…» (системный выбор файла, разрешения не нужны).

## Сборка

Требуется Android SDK и JDK 17. Путь к SDK — в `local.properties`
(`sdk.dir=C:/Users/GREAY/AppData/Local/Android/Sdk`).

```powershell
$env:GRADLE_USER_HOME = "E:\OpenCode\Noxis-IPTV\.gradle"   # готовый кэш Gradle из основного проекта
./gradlew.bat assembleRelease --no-daemon
```

Без ключа APK подпишется отладочным ключом — это нормально для проверки,
но в RuStore такой файл не примут.

### Сборка в GitHub Actions

Workflow: `.github/workflows/build.yml`. Запускается сам при push в `main`
(кроме правок только документации) или вручную: вкладка **Actions** → **Build APK (RuStore)**
→ **Run workflow**.

Что делает: юнит-тесты → `assembleRelease` → проверка, что в APK **нет**
`REQUEST_INSTALL_PACKAGES` → переименование в `noxis-iptv-rustore-<версия>.apk`
и SHA-256 → выгрузка во Artifacts.

**Готовый APK:** Actions → нужный запуск → блок **Artifacts** →
`noxis-iptv-rustore-apk.zip` (внутри APK и файл `.sha256`).

Подпись берётся из Secrets этого репозитория (Settings → Secrets and variables →
Actions → New repository secret). Нужны те же четыре секрета, что и в основном проекте:

| Секрет | Значение |
|---|---|
| `KEYSTORE_BASE64` | файл `release.keystore`, закодированный в base64 |
| `KEYSTORE_PASSWORD` | пароль хранилища ключей |
| `KEY_ALIAS` | имя ключа |
| `KEY_PASSWORD` | пароль ключа |

Base64 из файла ключа (PowerShell):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("E:\OpenCode\Noxis-IPTV\keys-backup\Noxis-IPTV\release.keystore"))
```

Ключ занимает **3488** символов base64 (2616 байт). Шаг «Ключ подписи из Secrets» печатает
длину полученного секрета и проверяет, что ключ открывается паролем `KEYSTORE_PASSWORD`, —
если длина меньше 3488, значит значение вставлено не целиком.

Без `KEYSTORE_BASE64` сборка пройдёт, но APK будет с отладочной подписью (в логе
будет предупреждение) — для RuStore он не годится.

Если при ручном запуске включить галочку **«Опубликовать релиз с APK»** и секреты
заданы, APK дополнительно появится в **Releases** (тег `rustore-<версия>-<номер запуска>`).

### Подпись постоянным ключом (локально)

Ключ и пароли лежат в основном проекте: `E:\OpenCode\Noxis-IPTV\keys-backup\Noxis-IPTV\`
(`release.keystore` и `secrets.txt`). В этот проект они **не копируются**.

Готовый скрипт: `tools/sign-release.ps1` — выравнивает APK (`zipalign`) и подписывает
(`apksigner`, схемы v1+v2+v3), затем кладёт файл в `releases/` и пишет SHA-256.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools\sign-release.ps1
```

Ожидаемый сертификат: `CN=Noxis IPTV, O=Noxis, C=BY`, SHA-256
`460cefeba99d0572712509ccd64b338af0f929e0816d33bfe8c4beb66378dc6a`.

## Публикация в RuStore

1. `versionCode` увеличивать перед каждой загрузкой (текущий — 35; сборки 27 и 28 отклоняли).
2. Загружать APK (не AAB): либо собранный в Actions (`noxis-iptv-rustore-3.2.4.apk`
   из Artifacts / Releases), либо локальный `releases/noxis-iptv-rustore-3.2.4.apk`.
3. В описании обновления перечислить изменения: три способа добавить источник
   (файл M3U, до 10 ссылок, список серверов), каждый плейлист отдельным пунктом
   в списке групп, файл server_IPTV.txt, удаление сохранённых файлов серверов, исправление листания ↑/↓ в своей группе.

## Проверка перед отправкой

```powershell
$aapt = "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\aapt2.exe"
& $aapt dump xmltree --file AndroidManifest.xml releases\noxis-iptv-rustore-3.2.4.apk | Select-String "REQUEST_INSTALL|WRITE_EXTERNAL"
```

Вывод должен быть пустым.

## Что куда

- `app/src/main/java/app/iptv/tv/UpdateFlow.kt` — заглушка обновлений для RuStore;
- `app/src/main/AndroidManifest.xml` — манифест без `REQUEST_INSTALL_PACKAGES` и `FileProvider`;
- `app/build.gradle.kts` — `applicationId`, версия и `UPDATES_ENABLED = false`;
- `app/src/main/res/drawable/icon.png`, `banner.png` — иконка и баннер с меткой RuStore;
- `tools/sign-release.ps1` — подпись готового APK.

## Репозиторий

GitHub: https://github.com/Ronis759/Noxis-IPTV-RuStore
(основной проект с автосборкой релизов — https://github.com/Ronis759/Noxis-IPTV)

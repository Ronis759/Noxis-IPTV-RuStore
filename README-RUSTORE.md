# Noxis IPTV — версия для RuStore

Отдельный проект: та же программа, но подготовленная под правила RuStore.
Основной проект (сборки с GitHub): `E:\OpenCode\Noxis-IPTV`.

## Чем отличается от обычной версии

| | Обычная (GitHub) | RuStore |
|---|---|---|
| `applicationId` | `app.iptv.tv` | `app.iptv.tv.rustore` |
| Название на ТВ | Noxis IPTV | Noxis IPTV RuStore |
| Иконка | «N» + цветная полоска | то же + зелёная метка RuStore в углу |
| Разрешение `REQUEST_INSTALL_PACKAGES` | есть | **нет** |
| Автообновление APK | есть | **нет** |
| Кнопка в настройках | «Проверить обновления» | «Обновите через RuStore» |
| Версия | 2.9.x (код = номер сборки GitHub) | 3.0.0 (код 28) |

Почему убрано автообновление: модерация RuStore (п. 2.14) не пропускает разрешение
`REQUEST_INSTALL_PACKAGES` — оно позволяет устанавливать APK в обход магазина.
Разрешение нужно было только встроенному автообновлению, поэтому оно удалено целиком:

- `AndroidManifest.xml` — нет `<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />`;
- нет `FileProvider` (он использовался только для запуска установщика APK);
- удалены `Updater.kt` (скачивание и запуск установщика) и `res/xml/file_paths.xml`;
- `UpdateFlow.kt` заменён на пустую версию: проверок нет, вместо установки — подсказка
  «Обновите приложение через RuStore»;
- `BuildConfig.UPDATES_ENABLED = false`.

## Сборка

Требуется Android SDK и JDK 17. Путь к SDK — в `local.properties`
(`sdk.dir=C:/Users/GREAY/AppData/Local/Android/Sdk`).

```powershell
$env:GRADLE_USER_HOME = "E:\OpenCode\Noxis-IPTV\.gradle"   # готовый кэш Gradle из основного проекта
./gradlew.bat assembleRelease --no-daemon
```

Без ключа APK подпишется отладочным ключом — это нормально для проверки,
но в RuStore такой файл не примут.

### Подпись постоянным ключом

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

1. `versionCode` увеличивать перед каждой загрузкой (текущий — 28; сборку 27 отклонили).
2. Загружать `releases/noxis-iptv-rustore-3.0.0.apk` (APK, не AAB).
3. В описании обновления указать: «Устранена причина отклонения: удалено разрешение
   REQUEST_INSTALL_PACKAGES, обновления устанавливаются через RuStore».

## Проверка перед отправкой

```powershell
$aapt = "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0\aapt2.exe"
& $aapt dump xmltree --file AndroidManifest.xml releases\noxis-iptv-rustore-3.0.0.apk | Select-String REQUEST_INSTALL
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

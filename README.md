# Саҥарар кинигэ

Android-приложение: якутская литература в исполнении актёров, с параллельным текстом
на якутском и русском.

**Стек:** Kotlin, Jetpack Compose, Material 3, Media3, Hilt, Room, Retrofit. minSdk 26, targetSdk 36.

## Как собрать APK

**В Android Studio:** File → Open → папка проекта → дождаться синхронизации →
кнопка ▶ Run (телефон подключён по USB с включённой «Отладкой по USB»).

**Из командной строки:** `./gradlew assembleDebug` → файл
`app/build/outputs/apk/debug/app-debug.apk`.

**Через GitHub:** вкладка Actions → последний запуск «Сборка APK» → Artifacts.

## Демо-версия

В приложение встроен демо-рассказ «Колобок» на якутском (`app/src/main/assets/demo/`):
аудио, якутский текст и русский перевод. Работает без интернета.
Каталог читается из `catalog.json` — позже тот же формат будет отдавать сервер.

## Структура

```
app/src/main/java/ru/sakhabooks/app/
  ui/theme       — цвета, шрифты (тёплая палитра)
  ui/components  — общие элементы, в т.ч. OrnamentBand (место под орнамент)
  ui/navigation  — адреса экранов и нижнее меню
  ui/<экран>     — экраны приложения (catalog, book — плеер и текст)
  data           — каталог книг и запоминание места остановки
app/src/main/res/values/strings.xml — все надписи интерфейса
```

## Перевод интерфейса на якутский

Скопировать `res/values/strings.xml` в `res/values-sah/strings.xml`, перевести значения,
добавить `<locale android:name="sah" />` в `res/xml/locales_config.xml`.

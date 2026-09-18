# BT Remote Pro

En native Android-app som återskapar den mörka BT Remote Pro-layouten och använder Androids
`BluetoothHidDevice`-profil för att kunna uppträda som en Bluetooth HID-enhet.

## Viktigt om Bluetooth
Telefonen måste stödja Bluetooth HID Device (API 28+ och OEM-stöd). Android TV måste kunna
ta emot HID/keyboard/consumer-control. Parkoppla TV:n med telefonen först och välj sedan TV:n
i appen.

Detta är en riktig native-app, inte Web Bluetooth. Web Bluetooth i en vanlig HTML-sida kan
inte ersätta Androids HID Device-profil.

## Bygg
Öppna projektet i Android Studio med internetanslutning för att hämta Android Gradle Plugin
och AndroidX. Bygg sedan `app` som APK.

## Begränsning
HID-profiler och vilka consumer-control-kommandon en viss Android TV accepterar varierar
mellan TV-tillverkare/Android-versioner. Appen visar anslutningsstatus och använder standard
HID consumer-control rapporter.


## Bygg APK direkt med GitHub Actions (utan Android Studio)

1. Skapa ett nytt GitHub-repository.
2. Ladda upp hela projektets innehåll till repositoryt (inte ZIP-filen som en enda fil).
3. Se till att `.github/workflows/build-apk.yml` också ligger i repositoryt.
4. Gå till **Actions** → **Build BT Remote Pro APK** → **Run workflow**.
5. När jobbet är klart: öppna körningen och hämta artifacten **BT-Remote-Pro-debug**.
6. Där finns `app-debug.apk`, som kan laddas ner till Android och installeras.

HTML-filen är gränssnittet/prototypen. Den native Android-appen är det som används för riktig
Bluetooth HID-kommunikation.

Scan-Utils

Kleine Tools um Fehler aus gescannten Texten zu entfernen.

Tools
-----

PreProcess
* Ersetzt Satzzeichen: doppelte Bindestriche durch m-breiten Bindestrich,
  << und >> durch « und », gerade Apostrophen durch typographische und
  einen Punkt in der Zeilenmitte durch einen normalen Punkt
* Fehlerhafte Ziffern und Buchstaben durch ? und ! ersetzen
* Brüche wie 1/2 durch ½ ersetzen
* überflüssige Bindestriche aus Wörtern entfernen
* Rechtschreibfehler aus einem Wörterbuch ersetzen

Aufruf: `textpreprocess.sh <Dateiname>`

Liest die Datei ein und führt die Aktionen durch. Das Ergebnis wird in die
Datei zurückgeschrieben. Die Original-Datei wird in `.1` umbenannt. Für die
Rechtschreibkorrektur wird ein Wörterbuch und eine Datei mit
Rechtschreibkorrekturen benötigt.

SpellCheck
* gibt falsch geschriebene Wörter in einer Datei aus

Aufruf: `spellcheck.sh <Dateiname>`

Alle Wörter in der Datei werden gegen ein Wörterbuch geprüft. Nicht gefundene
Wörter werden in `spellcheck.txt` ausgegeben.

CreateDictionary
* Erzeugt ein Wörterbuch aus Texten

Aufruf: `createdictionary.sh [Verzeichnis]`

`Verzeichnis` ist optional, als default wird das aktuelle Verzeichniss
verwendet.

Im Verzeichnis werden rekursiv alle Markdown-Dateien (Endung `.md`) gesucht. Es
wird eine Datei `german.dic` erstellt, die alle Wörten aus den Markdown-Dateien
enthält.

CheckCase
* korrigiert die Groß-/Kleinschreibung

Aufruf: `checkcase.sh <Dateiname>`

Ersetzt in der angegebenen Datei alle Wörter, die fälschlicherweise groß bzw.
klein geschrieben sind. Die Wörter, die korrigiert werden sollen, werden aus
der Datei `kleinschreibung.txt` ausgelesen.

KoboAnnotationExtractor
* Extrahiert aus den Annotations des Kobo-eReaders die Texte und gibt sie sortiert aus.

Aufruf: `koboAnnotationExtractor.sh <Dateiname>`

Dateien
-------

Als Wörterbuch dient eine Datei, die pro Zeile ein Wort enthält. Die Datei muss
`german.dic` heißen und wird im Verzeichnis gesucht, in dem der Text steht. Ist
die Datei dort nicht vorhanden, wird sie in den Parent-Verzeichnissen gesucht.

Für Rechtschreibkorrekturen wird eine Datei `rechtschreibung.csv` nach den
selben Regeln gesucht. Die Datei enthält durch Leerzeichen oder Tab getrennt,
ein falsch geschriebenes Wort und die korrigierte Fassung.

Für die Korrektur der Groß-/Kleinschreibung wird eine Datei namens
`kleinschreibung.txt` gesucht. Sie enthält pro Zeile ein Wort, dass, außer am
Satzanfang, immer groß geschrieben wird.

Scan-Utils

Kleine Tools um Fehler aus gescannten Texten zu entfernen.

Tools
-----

__PrepareText__
* Entfernt Schmierzeichen
* ersetzt einige Sonderzeichen im Text
* ersetzt einmalig bestimmte Wörter

Aufruf: `prepareText.sh <Dateiname(n)>`

Das Tool sollte einmalig vor der eigentlichen Bearbeitung gestartet werden.
Es werden Schmierzeichen wie Slashes und Pipe-Zeichen am Anfang und Ende der
Zeile entfernt, und einige Sonderzeichen in Anführungszeichen bzw. Bindestriche
umgewandelt.

Wort-Ersetzungen, die in `replace_once.txt` aufgeführt sind, werden durchgeführt.
Dies sind Ersetzungen, die auch richtig geschriebene Wörter durch eine Korrektur
austauschen. Beispielsweise macht die OCR oft aus "Jahren" "Fahren". Mit diesem
Mechanismus werden diese Fehler einmalig korrigiert. Wenn zuviel korrigiert wurde,
muß dies anschließend von Hand rückgängig gemacht werden.

__ImportKabel__
Das Tool lädt Texte von der <a href="https://www.walther-kabel.de/">Kabel-Seite</a>
herunter und bereitet sie auf:

* ersetzt gerade Apostrophen durch typographische
* fügt Referenzen auf Fußnoten ein
* packt Kapitel-Überschriften in h2/h3-Tags

Aufruf: `importKabel.sh <URL>`

__PreProcess__
* Ersetzt Satzzeichen: doppelte Bindestriche durch m-breiten Bindestrich,
  << und >> durch « und », gerade Apostrophen durch typographische und
  einen Punkt in der Zeilenmitte durch einen normalen Punkt
* Fehlerhafte Ziffern und Buchstaben durch ? und ! ersetzen
* Brüche wie 1/2 durch ½ ersetzen
* überflüssige Bindestriche aus Wörtern entfernen
* Rechtschreibfehler aus einem Wörterbuch ersetzen
* gängige Vertauschungen von Buchstaben korrigieren (z.B. Waffer -> Wasser)

Aufruf: `textpreprocess.sh [Optionen] <Dateiname(n)>`

Liest die Datei ein und führt die Aktionen durch. Das Ergebnis wird in die
Datei zurückgeschrieben. Die Original-Datei wird in `.1` umbenannt. Für die
Rechtschreibkorrektur wird ein Wörterbuch und eine Datei mit
Rechtschreibkorrekturen benötigt.

Die geänderten Wörter werden in der Datei `changes.log` protokolliert.

Optionen:
* -[n] gibt an, wieviel Aufwand getrieben werden soll, um Wörter gegen das Wörterbuch
  abzugleichen. Ein höherer Wert steht dabei für einen höheren Aufwand. Default: 6

__SpellCheck__
* gibt falsch geschriebene Wörter in einer Datei aus

Aufruf: `spellcheck.sh [-<threshold>] [-f] <Dateiname(n)>`

Alle Wörter in der Datei werden gegen ein Wörterbuch geprüft. Nicht gefundene
Wörter werden in `spellcheck.log` ausgegeben.

Wenn angegeben, werden nur Wörter ausgegeben, die mindestens <threshold> mal
vorkommen.

Wird die Option `-f` angegeben, werden die Dateinamen aus den nachfolgenden
Dateien zeilenweise eingelesen.

__CreateDictionary__
* Erzeugt ein Wörterbuch aus Texten

Aufruf: `createdictionary.sh [Verzeichnis]`

`Verzeichnis` ist optional, als default wird das aktuelle Verzeichniss
verwendet.

Im Verzeichnis werden rekursiv alle Markdown-Dateien (Endung `.md`) gesucht. Es
wird eine Datei `german.dic` erstellt, die alle Wörter aus den Markdown-Dateien
mit ihrer Häufigkeit enthält.

__UpdateDictionary__
* Aktualisiert die Häufigkeiten für ein Wörterbuch aus den Texten

Aufruf: `updatedictionary.sh [Verzeichnis]`

`Verzeichnis` ist optional, als default wird das aktuelle Verzeichniss
verwendet.

Im Verzeichnis werden rekursiv alle Markdown-Dateien (Endung `.md`) gesucht. Es
wird die Datei `german.dic` eingelesen und die Häufigkeiten der Wörter aktualisiert.
Es werden keine neuen Wörter zum Wörterbuch hinzugefügt.

__CheckCase__
* korrigiert die Groß-/Kleinschreibung

Aufruf: `checkcase.sh <Dateiname(n)>`

Ersetzt in der angegebenen Datei alle Wörter, die fälschlicherweise groß bzw.
klein geschrieben sind. Die Wörter, die korrigiert werden sollen, werden aus
der Datei `german.dic` ausgelesen. Wörter, die sowohl groß als auch klein
geschrieben vorhanden sind, werden ignoriert.

Fehlende Punkte am Absatzende werden eingefügt.

__CheckQuotes__
* Prüft die öffnenden und schließenden Anführungszeichen in einer Datei.

Aufruf: `checkquotes.sh <Dateiname(n)>`

Prüft die öffnenden und schließenden Anführungszeichen in den angegebenen Dateien.
Die Ergebnisse der Prüfung werden in der Datei `checkquotes.log` ausgegeben.

__KoboAnnotationExtractor__
* Extrahiert aus den Annotations des Kobo-eReaders die Texte und gibt sie sortiert
in der Datei `annotations.log` aus.

Aufruf: `koboAnnotationExtractor.sh <Dateiname(n)>`

Dateien
-------

Als Wörterbuch dient eine Datei, die pro Zeile ein Wort und dessen Häufigkeit enthält.
Das Wort ist durch ein Tab von der Häufigkeit getrennt, fehlt die Häufigkeit, wird 1 angenommen.
Die Datei muss `german.dic` heißen und wird im Verzeichnis gesucht, in dem der Text steht. Ist
die Datei dort nicht vorhanden, wird sie in den Parent-Verzeichnissen gesucht.
Sind mehrere Dateien in mehreren Verzeichnissen vorhanden, werden sie alle eingelesen.

Für Rechtschreibkorrekturen wird eine Datei `rechtschreibung.csv` nach den
selben Regeln gesucht. Die Datei enthält durch Leerzeichen oder Tab getrennt,
ein falsch geschriebenes Wort und die korrigierte Fassung.

Silben, die bei der Wort-Korrektur nicht verändert werden sollen, werden in
`silben.dic` gespeichert.

Einmalige Ersetzungen werden in der Datei `replace_once.txt` aufgelistet.
Die Datei besteht aus einem regulären Ausdruck, einem Tab und der Ersetzung.


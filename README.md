Scan-Utils

Kleine Tools um Fehler aus gescannten Texten zu entfernen.

Tools
-----

__PrepareText__
* Entfernt Schmierzeichen
* ersetzt einige Sonderzeichen im Text

Aufruf: `prepareText.sh <Dateiname(n)>`

Das Tool sollte einmalig vor der eigentlichen Bearbeitung gestartet werden.
Es werden Schmierzeichen wie Slashes und Pipe-Zeichen am Anfang und Ende der
Zeile entfernt, und einige Sonderzeichen in Anführungszeichen bzw. Bindestriche
umgewandelt.

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

Aufruf: `spellcheck.sh [-<threshold>] <Dateiname(n)>`

Alle Wörter in der Datei werden gegen ein Wörterbuch geprüft. Nicht gefundene
Wörter werden in `spellcheck.log` ausgegeben.

Wenn angegeben, werden nur Wörter ausgegeben, die mindestens <threshold> mal
vorkommen.

__CreateDictionary__
* Erzeugt ein Wörterbuch aus Texten

Aufruf: `createdictionary.sh [Verzeichnis]`

`Verzeichnis` ist optional, als default wird das aktuelle Verzeichniss
verwendet.

Im Verzeichnis werden rekursiv alle Markdown-Dateien (Endung `.md`) gesucht. Es
wird eine Datei `german.dic` erstellt, die alle Wörten aus den Markdown-Dateien
enthält.

__CheckCase__
* korrigiert die Groß-/Kleinschreibung

Aufruf: `checkcase.sh <Dateiname(n)>`

Ersetzt in der angegebenen Datei alle Wörter, die fälschlicherweise groß bzw.
klein geschrieben sind. Die Wörter, die korrigiert werden sollen, werden aus
der Datei `german.dic` ausgelesen. Wörter, die sowohl groß als auch klein
geschrieben vorhanden sind, werden ignoriert.

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

Als Wörterbuch dient eine Datei, die pro Zeile ein Wort enthält. Die Datei muss
`german.dic` heißen und wird im Verzeichnis gesucht, in dem der Text steht. Ist
die Datei dort nicht vorhanden, wird sie in den Parent-Verzeichnissen gesucht.
Sind mehrere Dateien in mehreren Verzeichnissen vorhanden, werden sie alle eingelesen.

Für Rechtschreibkorrekturen wird eine Datei `rechtschreibung.csv` nach den
selben Regeln gesucht. Die Datei enthält durch Leerzeichen oder Tab getrennt,
ein falsch geschriebenes Wort und die korrigierte Fassung.

Silben, die bei der Wort-Korrektur nicht verändert werden sollen, werden in
`silben.dic` gespeichert.


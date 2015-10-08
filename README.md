# launchpad

A Clojure library for interacting with a Novation Launchpad.

Tested with a new Launchpad Mini in 2015, which I understand works the same as
a Launchpad S, but smaller. It may work for an older model Launchpad too, or
for a Launchpad Pro. If you have one of those and try it, let me know how it
goes.

## Usage

```clojure
(require '[launchpad.core :as lp])

;; Connect to and initialize the Launchpad
(def pad (lp/make-model))

;; blinkenlights
(.grid pad [x y] [red green]) ; light a grid button
(.top pad x [red green])      ; light a top-row button
(.side pad y [red green])     ; light a side-column button

;; start over
(.reset pad)
```

Colors are `[red green]` where `red` and `green` range from
0 (off) to 3 (brightest). Red and green mix to yield amber.

Coordinates are 0-indexed starting from the top-left.

## Gotchas
There is a Java bug on OSX: Java caches the MIDI device list indefinitely. So
you need to have the Launchpad plugged in before starting the program/repl.

## TODO
This initial release only allows you to send commands to the Launchpad.
Next up: allow you to respond to button presses by the user!

## License

Copyright © 2015 Hans Fugal

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

![A Launchpad Mini](http://global.novationmusic.com/sites/default/files/styles/cta_scale_1280/public/2-LP-Mini_1%5B1%5D.png)
# launchpad

A Clojure library for interacting with a Novation Launchpad.

Tested with a new
[Launchpad Mini](http://global.novationmusic.com/launch/launchpad-mini) in
2015, which I understand works the same as a Launchpad S, but smaller. It may
work for an older model Launchpad too. The new full-size Launchpad and
Launchpad Pro with RGB colors could probably be made to work, but I don't have
one to test on so I'm ignoring it. If you're interested and can help test,
reach out. 

## Usage

```clojure
(require '[launchpad :as lp])

;; blinkenlights
(lp/grid [x y] [red green]) ; light a grid button
(lp/top x [red green])      ; light a top-row button
(lp/right y [red green])    ; light a right side-column button

;; start over
(lp/reset)
```

Colors are `[red green]` where `red` and `green` range from
0 (off) to 3 (brightest). Red and green mix to yield amber.

Coordinates are 0-indexed starting from the top-left.

There are examples for your perusal in `src/examples`.

## Advanced Usage

Namespace `launchpad` will get you going, but for full power you'll want to use
`launchpad.core`.

You can provide a reactor to the Launchpad, which is just a callback that will
react to button presses, e.g. `[:grid [x y] [r g]]`. If your reactor returns a
state, the Launchpad will update accordingly. Here's an example sketch, and you
can see `src/examples/roxanne.clj` for a more complete example.

```clojure
(defn next-state [state]
  ...)

(defn my-reactor [state what where velocity]
  (println [what where velocity])
  (next-state state))

(.react pad my-reactor)
```

## Gotchas
There is a Java bug on OSX: Java caches the MIDI device list indefinitely. So
you need to have the Launchpad plugged in before calling `get-launchpad`, else
you have to restart the JVM.

## TODO
Finish examples, and maybe make more (at least Knight Rider).

## License

Copyright © 2015 Hans Fugal

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

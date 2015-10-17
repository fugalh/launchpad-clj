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

Namespace `launchpad` will get you going, but for full power you'll want to use
`launchpad.core` and do batch updates of state (much faster - see
`src/examples/randgrid.clj`.

## Gotchas
There is a Java bug on OSX: Java caches the MIDI device list indefinitely. So
you need to have the Launchpad plugged in before calling `get-launchpad`, else
you have to restart the JVM.

## TODO
Finish examples, and maybe make more (at least Knight Rider).
Figure out whether we can do piecemeal updates with reasonable performance.

## License

Copyright © 2015 Hans Fugal

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

(ns examples.randgrid
  (:require [launchpad :refer :all]))

(defn main
  "At random intervals, light random buttons, ad infinitum."
  [max-interval-secs]
  (let [pad (get-launchpad)
        max-interval (* 1000 max-interval-secs)]
    (while true
      (let [x (rand-int 9)
            y (dec (rand-int 9))
            ms (rand-int max-interval)
            r (rand-int 4)
            g (rand-int 4)
            ]
        (when-not (= [8 8] [x y]) ; no button here
          (if (= -1 y)
            (.light pad :top x [r g])
            (if (= 8 x)
              (.light pad :side y [r g])
              (.light pad :grid [x y] [r g])))
          (Thread/sleep ms))))))

(defn -main
  ([]
   (main 1))
  ([max-interval]
   (main (read-string max-interval))))

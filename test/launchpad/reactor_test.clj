(ns launchpad.reactor-test
  (:require [clojure.test :refer :all]
            [launchpad.reactor :as r]))

(deftest translate-message
         (let [x (rand-int 8)
               y (rand-int 8)
               yy (* 0x10 y)
               xy (+ x yy)
               t (+ 0x68 x)
               grid (r/translate-message (midi/note-on xy 127))
               top (r/translate-message (midi/control-change t 127))
               right (r/translate-message (midi/note-on (+ 8 yy) 127))]

           (is grid)
           (is top)
           (is right)

           (is (= :grid (.type grid)))
           (is (= :top (.type top)))
           (is (= :right (.type right)))

           (is (= [x y] (.loc grid)))
           (is (= x (.loc top)))
           (is (= y (.loc right)))))

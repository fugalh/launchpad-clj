;; example of doing chuck-like shreds


;; a shred is chuck-speak for a "fiber" or whatever you want to call it (goroutine, go block, etc.)
;; so light-row will return immediately but take period seconds to actually light the row

(require '[launchpad.core :refer :all]
         '[clojure.core.async :as a])

;; has to be a macro for go to stay in scope? defn didn't work, at least. concerning :/
(defmacro wait [millis] `(a/<! (a/timeout ~millis)))

(defn light-row
  "shred to light the given row from left to right over the given period"
  [lp row col secs]
  (a/go
   (let [ms (* 1000 (/ secs 7))]
     (light lp 0 row col)
     (dotimes [n 7]
       (wait ms)
       (light lp (+ n 1) row col)))))
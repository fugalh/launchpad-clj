;;; Display a spectrum analysis, a la ye olde WinAmp
;;; We read a block of audio from the default system input, do an FFT,
;;; bin by octaves, and display octaves 1-8 (which corresponds roughly
;;; to the piano keyboard range)

(ns examples.spectrum_analyzer
  (:require [launchpad.core :as lp])
  (:import (javax.sound.sampled AudioFormat
                                AudioSystem))
  (:gen-class))

;;; The FFT stuff. (Could I use a library? Of course, but there wasn't
;;; a simple nice one I could find in a quick search, and I hadn't
;;; implemented an FFT in awhile...)

;;; 2048 samples gives a bin width
;;; and frame rate of a little over 20 Hz (but we overlap to display
;;; at 40 Hz)
(def N 2048)

(def lg
  "Log base 2"
  (memoize
   (fn [x] (/ (Math/log x) (Math/log 2)))))

(def octave-bins
  "Map the FFT bins to Octave bins. For N=2048 there are 11 bins."
  (let [o (+ 1 (lg N))
        octaves (map #(Math/pow 2 %) (range o))]
    (map vector
         (drop-last octaves)
         (drop 1 octaves))))

;;; Thanks for not having a complex type, Java. wtf
;;; In this file, a complex number is represented as (a + bi) = [a b]

(defn expi
  "e^(i*x) with kudos to Euler"
  [x]
  [(Math/cos x) (Math/sin x)])

(defn +i
  [[a b] [c d]]
  [(+ a c) (+ b d)])

(defn -i
  [[a b] [c d]]
  [(- a c) (- b d)])

(defn *i
  [[a b] [c d]]
  [(- (* a c)
      (* b d))
   (- (* a d)
      (* b c))])

(def twiddle
  "FFT twiddle factors"
  (memoize (fn [k n]
             (expi (* -2 Math/PI (/ k n))))))
   
(defn fft
  "Return the FFT of the input block, whose length (N) must be a power
  of two. The resulting sequence is complex."
  ([block]
   (let [n (count block)
         n2 (/ n 2)]
     (if (= 1 n)
       [[(first block) 0]] ; base case
       (let [block1 (fft (take-nth 2 block)) ; evens
             block2 (fft (take-nth 2 (rest block)))] ; odds
         (concat (map (fn [k x1 x2]
                        (+i x1
                            (*i (twiddle k n)
                                x2)))
                      (range n2)
                      block1
                      block2)
                 (map (fn [k x1 x2]
                        (-i x1
                            (*i (twiddle k n)
                                x2)))
                      (range n2)
                      block1
                      block2)))))))

(defn magnitude
  "Convert the sequence of complex to magnitude"
  [spectrum]
  (map (fn [[a b]]
         (Math/sqrt (+ (* a a)
                       (* b b))))
       spectrum))

(defn normalize-to
  "Normalize magnitude to the given maximum."
  [spectrum max-mag]
  (if (= 0.0 max-mag) ; NB 0 doesn't work, only 0.0!
    spectrum
    (map #(/ % max-mag) spectrum)))

(defn bin-by-octave
  [spectrum]
  (let [spectrum (vec spectrum)]
    (map (fn [[a b]]
           (/ (reduce + (subvec spectrum a b)) (- b a)))
         octave-bins)))

(defn dBFS
  [x]
  (* 20 (Math/log10 (Math/abs x))))

(defn ->dBFS
  "collection to dBFS"
  [coll]
  (map dBFS coll))

(def hann-window
  (memoize
   (fn [n]
     (let [k #(Math/sin (/ (* Math/PI %)
                           (dec n)))]
       (map k (range n))))))

(defn window-by
  [block window]
  (map * block window))

(defn open-audio
  []
  (let [af (AudioFormat. 44100 8 1 true false)
        tdl (AudioSystem/getTargetDataLine af)]
    (.open tdl af)
    (.start tdl)
    tdl))

(defn read-audio
  [tdl n]
  (let [buf (byte-array n)]
    (.read tdl buf 0 n)
    (vec buf)))

;;; The Launchpad stuff
(defn colorize
  "The color for a cell"
  [x y]
  ([[3 0]
    [3 1]
    [3 2]
    [3 3]
    [2 3]
    [1 3]
    [0 3]
    [0 3]] y))

(defn render-column
  [c dbfs]
  (let [y (- (int (max -8 dbfs)))]
    (if (<= 0 y 7)
      (reduce merge {}
              (for [yy (range y 8)] {[c yy]
                                     (colorize c yy)})))))
        
(defn render-spectrum
  "Render the spectrum to a Launchpad state"
  [spectrum]
  (let [s lp/initial-state
        db-range 96 ; 16-bit resolution (ignore dithering)
        ;; but really music is usually only about half that dynamic range
        db-range 50
        ;; map the dynamic range to the 8-high grid
        spectrum (map #(* 8 (/ % db-range)) spectrum)]
    (assoc s :grid
           (reduce merge {}
                   (map render-column
                        (range 8)
                        spectrum)))))

;;; run
(defn run []
  (let [tdl (open-audio)
        pad (lp/get-launchpad)]
    (.reset pad)
    ;; We adapt the range of the spectrum by the maximum observed
    ;; magnitude and the dynamic range (in render-spectrum). In this
    ;; way we completely punt on the question of "what does it mean to
    ;; normalize an FFT?", and it's pretty much self-adjusting.
    ;;
    ;; We also overlap two N/2 reads to double the framerate
    (loop [max-mag 0
           xs1 (read-audio tdl (/ N 2))]
      (let [xs2 (read-audio tdl (/ N 2))
            xs (concat xs1 xs2)
            spectrum (-> xs
                         (window-by (hann-window N))
                         (fft) ; N wide
                         (magnitude)
                         (bin-by-octave)) ; 11 wide
            max-mag (apply max max-mag spectrum)
            spectrum (normalize-to spectrum max-mag)
            spectrum (->dBFS spectrum)]
        (.render pad (render-spectrum spectrum))
        (recur max-mag xs2)))))

(defn -main
  []
  (run))

;;; TODO
;;; looks like double-buffering might help smooth display
;;; falling peaks
;;; dynamic range zoom in/out (/8 or *8)

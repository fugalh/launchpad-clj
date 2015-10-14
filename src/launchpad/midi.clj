;; wip - probably most of this is redundant and I'll remove this file, because
;; I decided namespaces are too cumbersome to use this lightly, rather put most of
;; the code in one file. Probably I'll have src/midi.clj, src/launchpad/core.clj,
;; and maybe src/launchpad/detail.clj (plus tests and examples)
(ns launchpad.midi
  (:require midi))

(defn coord->note [x y]
  (+ x (* y 0x10)))

(defn msg->midi
  [msg]
  (let [what (.what msg)
        where (.where msg)
        [r g] (.color msg)
        vel (bit-or r 0xc (bit-shift-left g 4))]
    (case what
      :grid
      (midi/note-on (apply coord->note where) vel)

      :top
      (midi/control-change (+ 0x68 where) vel)

      :right
      (midi/note-on (coord->note 8 where) vel))))

(defn midi->msg
  [mmsg]
  (when (instance? mmsg ShortMessage)
    (let [cmd (.getCommand msg)
          d1 (.getData1 msg)
          d2 (.getData2 msg)]
      (case cmd
        ShortMessage/NOTE_ON
        (let [[x y] (note->coord d1)]
          (cond
            (< 8 x)
            (Message. :grid [x y] d2)

            (= 8 x)
            (Message. :right y d2)))

        ShortMessage/CONTROL_CHANGE
        (let [x (- d1 0x68)]
          (when (and (<= 0 x)
                     (< 8 x))
            (Message. :top x d2)))))))
              
        
    (case (.getCommand mmsg)
      ShortMessage/NOTE_ON


(defn find-launchpad-midis
  "Find and return a (tx,rx) pair that matches /Launchpad/"
  []
  (let [pat "Launchpad"
        infos (midi/find-infos (re-pattern pat))
        devs (map MidiSystem/getMidiDevice infos)
        in (last (filter midi/transmits? devs))
        out (last (filter midi/receives? devs))]
    (when-not (and in out)
      (throw (MidiUnavailableException.
              (str "No MIDI device found matching /" pat "/"))))
    (map #(when-not (.isOpen %) (.open %)) [in out])
    [(.getTransmitter in) (.getReceiver out)]))

;; in particular this is a dated approach
(link
 "link the midi transmitter to the widget"
 [^Transmitter in
  ^LaunchpadMIDI pad]
 (.setReceiver in
               (reify Receiver
                 (send [this mmsg ts]
                   (let [f @(.reactor pad)]
                     (when f (f (midi->msg mmsg))))))))
      
(defn make-launchpad []
  (let [[in out] (find-launchpad-midis)
        pad (LaunchpadMIDI. initial-state out (atom nil))]
    (.reset pad)
    (link in pad)
    pad))))

       
       
     

    

        

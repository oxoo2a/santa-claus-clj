(ns santa-claus.core (:gen-class))

(require '[clojure.core.async :as async :refer [>! <! >!! <!! go chan buffer close! thread alts! alts!! timeout]])

;; *****************************************************************************
;; Barrier
;; *****************************************************************************

(defn barrier-code [b-in b-out n]
  (while true
    (dotimes [c n] (async/<!! b-in))
    (dotimes [c n] (async/>!! b-out 1))
    )
  )

(defn barrier-init [n]
  (let [b-in (async/chan)
        b-out (async/chan)
        ]
    [b-in b-out (async/thread (barrier-code b-in b-out n))]
    )
  )

(defn barrier-reached [b]
  (let [b-reached (get b 0)
        b-wait (get b 1)
        c (get b 2)
        ]
    (async/>!! b-reached 1)
    (async/<!! b-wait)
    )
  )

(defn barrier-close [b]
  (async/<!! (get b 2))
  )

;; *****************************************************************************
;; Serialize println
;; *****************************************************************************

(def printer (atom nil))

(defn print-thread []
  (while true
    (println (async/<!! @printer))))

(defn printer-init []
  (reset! printer (async/chan))
  (async/thread (print-thread))
  )

(defn print-a-line [s]
  (async/>!! @printer s)
 ;  (println s)
  )

;; *****************************************************************************
;; Application-specific code
;; *****************************************************************************

;; -----------------------------------------------------------------------------
;; Butler

(def number-of-elves-waiting-for-help (atom 0))

(defn butler-init []
  { :santa-channel (async/chan) :elves-channel (async/chan) :workbench (barrier-init 2) })

(defn do-not-disturb [butler]
  (async/<!! (get butler :santa-channel)))

(defn its-christmas [butler]
  (async/>!! (get butler :santa-channel) "Reindeer"))

(defn i-need-help [butler]
  (let [n-elves (swap! number-of-elves-waiting-for-help inc)]
    (if (>= n-elves 3)
      (async/>!! (get butler :santa-channel) "Elves"))) ; at least 3 elves are needed to wake up Santa Claus
  )

(defn get-help [butler name]
    (async/<!! (get butler :elves-channel))
    (barrier-reached (get butler :workbench))
    (print-a-line (str "Elf: Santa and i (" name ") solve the problem"))
    (barrier-reached (get butler :workbench))
    )

(defn help-elves [butler]
    (dotimes [e (min @number-of-elves-waiting-for-help 3)]
      (async/>!! (get butler :elves-channel) "Lets go")
      (swap! number-of-elves-waiting-for-help dec)
      (barrier-reached (get butler :workbench))
      (print-a-line "Santa: I am here to help ;-)")
      (Thread/sleep 500)
      (barrier-reached (get butler :workbench))
      )   
  )

;; -----------------------------------------------------------------------------

(def reindeer-names `["R1" "R2" "R3"])
(def elf-names `["E1" "E2" "E3" "E4" "E5" "E6"])

(def still-running (atom true))

(defn reindeer-main [name args]
  (let [northpole (first args)
        sleigh (first (rest args))
        butler (first (rest (rest args)))]
    (while (= @still-running true)
        (print-a-line (str "Reindeer " name " is on vacation"))
        (Thread/sleep (+ 5000 (rand 3000)))
        (barrier-reached northpole)
        (print-a-line (str "Reindeer " name " is back at northpole"))
        (if (= name (first reindeer-names))
          (do
            (print-a-line (str "All reindeer waiting for sleigh"))
            (its-christmas butler)
            )
          )
        (barrier-reached sleigh)
      ; gift giving around the world
        (barrier-reached sleigh)
        (if (= name (first reindeer-names)) (print-a-line (str "All reindeer left sleigh")))
    )
  )
)

(defn santa-main [name args]
  (let [sleigh (first args)
        butler (first (rest args))]
    (print-a-line (str "This is " name))
    (while (= @still-running true)
      (let [reason (do-not-disturb butler)]
        (print-a-line "Here is Santa Claus, who woke me up?")
        (if (= reason "Reindeer")
          (do
            (barrier-reached sleigh)
            (print-a-line "Reindeers! It's time for gift giving ...")
            (Thread/sleep 2000)
            (print-a-line " ... finished")
            (barrier-reached sleigh))
          (if (= reason "Elves")
            (do
              (print-a-line "Elves! Let's help them")
              (help-elves butler)
              )
            (print-a-line (str "Unknown reason " reason " to wake me up")))))
      )
    )
  )

(defn elf-main [name args]
  (let [butler (first args)]
    (print-a-line (str "Elf " name " ready to craft gifts"))
    (while (= @still-running true)
      (Thread/sleep (+ 10000 (rand 10000)))
      (print-a-line (str "Elf " name " needs help"))
      (i-need-help butler)
      (get-help butler name)
      )
    )
  )

;; *****************************************************************************
;; Thread management
;; *****************************************************************************

(defn start-a-thread [f id & args] (async/thread (f id (first args))))

(defn thread-group [code names & args] (doall (map (fn [n] (start-a-thread code n args)) names))) ; doall enforces evaluation

(defn join-all [threads] (doall (map (fn [t] (async/<!! t)) threads)))

;; *****************************************************************************
;; Main
;; *****************************************************************************

(defn -main
  "This is the Clojure version of the Santa Claus Problem"
  [& args]
  
  (printer-init)
  (print-a-line "Santa Claus is about to start ...")
  
  (let
   [northpole (barrier-init (count reindeer-names))
    sleigh (barrier-init (inc (count reindeer-names)))
    butler (butler-init)
    reindeer (thread-group reindeer-main reindeer-names northpole sleigh butler)
    santa (thread-group santa-main `["Santa Claus"] sleigh butler)
    elves (thread-group elf-main elf-names butler)]

    (Thread/sleep 30000)
    
    (print-a-line "---------- Telling all threads to stop! ----------")
    (reset! still-running false)
    (join-all reindeer)
    (join-all santa)
    (join-all elves)
    )
  "Done"
  )

; (-main)
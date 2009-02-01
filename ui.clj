;   Copyright (c) Zachary Wilson. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file COPYING at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns imagesieve.ui
  (:use imagesieve.logic imagesieve.image zutil.util)
  (:import (javax.swing JFileChooser JFrame JPanel JLabel UIManager JButton JOptionPane WindowConstants)
           (java.awt.event ActionListener)
           (java.io File))
  (:gen-class))
 
(in-ns 'imagesieve.ui)

; I hate the old GTK file chooser.
; TODO - use native look and feel except with file chooser.

(when-not (= "com.sun.java.swing.plaf.gtk.GTKLookAndFeel"
             (UIManager/getSystemLookAndFeelClassName))
  (UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName)))

(defn error-message [e]
  (JOptionPane/showMessageDialog nil
                                 (.getMessage e)
                                 "Error"
                                 (JOptionPane/ERROR_MESSAGE)))
  
(def *count* (ref 0))
 
(def *max* (atom 0))
 
(def *status* (JLabel. "0/0 completed    "))
 
(defn update-counter [& args]
  (dosync (alter *count* inc)
          (.setText *status*
                    (str @*count* "/" @*max* " completed"))))

 
(defn choose-and-go []) ; Placeholder for frame.
 
(def frame ; The main UI window - defined at the toplevel so the JFileChooser can refer to it.
     (doto (JFrame. "Greyscale Converter")
       (.add (doto (JPanel.)
               (.add (doto (JButton. "Convert a folder to greyscale")
                       (.addActionListener
                        (proxy [ActionListener] []
                          (actionPerformed [e] (choose-and-go))))))
               (.add *status*)))
       (.setDefaultCloseOperation (WindowConstants/EXIT_ON_CLOSE))
       (.pack)))
 
; Persistant file chooser so it remembers the last dir.

(def chooser (doto (JFileChooser.)
               (.setFileSelectionMode (JFileChooser/DIRECTORIES_ONLY))))
 
(defn process-and-update [img]
  (make-grey! img)
  (update-counter))

(defn do-processing [state path]
 (try (doall (process-dir process-and-update
                          path
                          (str path "-grey/")))
      (catch Exception e (error-message e)))
 nil)

(def #^{:doc "This agent exists to run the processing from a separate thread.
      Doing so instead of having a direct callback from the JFileChooser
      makes the counter in the UI update in real-time."}
     *processor*
     (agent nil))

(defn handle-dir [path]
  (swap! *max* identity*
         (count (filter image-format
                        (.list (File. path)))))
  (dosync (ref-set *count* -1))
  (update-counter)
  (send-off *processor* do-processing path)
  (when (agent-errors *processor*)
    (error-message (first (agent-errors *processor*)))))

(defn choose-and-go []
  (let [status (.showOpenDialog chooser frame)]
    (when (= (JFileChooser/APPROVE_OPTION) status)
      (try
       (let [path (.getAbsolutePath (.getSelectedFile chooser))
             dir (File. path)]
         (if (and (.exists dir) (.isDirectory dir))
           (handle-dir path)
           (JOptionPane/showMessageDialog nil
                                          "Sorry, I couldn't find the directory you asked for"
                                          "Alert"
                                          (JOptionPane/ERROR_MESSAGE))))
       (catch Exception e (error-message e))))))
 
(defn -main []
  (. frame show))

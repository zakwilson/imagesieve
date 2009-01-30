(ns imagesieve.ui
  (:use imagesieve.logic imagesieve.image zutil.util)
  (:import (javax.swing JFileChooser JFrame JPanel JTextField UIManager JButton JOptionPane WindowConstants)
           (java.awt.event ActionListener)
           (java.io File))
  (:gen-class))

(in-ns 'imagesieve.ui)

(when-not (= "com.sun.java.swing.plaf.gtk.GTKLookAndFeel" 
             (UIManager/getSystemLookAndFeelClassName))
  (UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName)))


(def *count* (agent 0))

(def *max* (agent 0))

(def *status* (agent (JTextField. "0/0 completed")))

(defn update-counter [& args]
  (send-off *count* inc)
  (send-off *status* (fn [textfield & args]
                       (.setText textfield
                                 (str @*count* "/" @*max* " completed"))
                       textfield)))

(defn choose-and-go []) ;placeholder

(def frame (doto (JFrame. "Greyscale Converter")
             (.add (doto (JPanel.)
                     (.add (doto (JButton. "Convert a folder to greyscale")
                             (.addActionListener
                              (proxy [ActionListener] []
                                (actionPerformed [e] (choose-and-go))))))
                     (.add @*status*)))
             (.setDefaultCloseOperation (WindowConstants/EXIT_ON_CLOSE))
             (.pack)))

(def chooser (doto (JFileChooser.)
               (.setFileSelectionMode (JFileChooser/DIRECTORIES_ONLY))))

(defn process-and-update [img]
  (make-grey! img)
  (update-counter)
  img)

(defn error->str [e]
  (str (.getMessage e) ", "))

(defn choose-and-go [] 
  (let [status (.showOpenDialog chooser frame)]
    (when (= (JFileChooser/APPROVE_OPTION) status)
      (try
       (let [path (.getAbsolutePath (.getSelectedFile chooser))
             dir (File. path)]
         (if (and (.exists dir) (.isDirectory dir))
           (do (send-off *max* identity* (count (.list dir)))
               (send-off *count* identity* -1)
               (update-counter)
               (doall (process-dir process-and-update ;shouldn't be lazy, but doesn't always complete
                                   path
                                   (str path "-grey/"))))
          (JOptionPane/showMessageDialog nil
                                         "Sorry, I couldn't find the directory you asked for"
                                         "Alert"
                                         (JOptionPane/ERROR_MESSAGE))))
       (catch Exception e (JOptionPane/showMessageDialog nil
                                                         (.getMessage e)
                                                         "Error"
                                                         (JOptionPane/ERROR_MESSAGE)))))))

(defn -main [] 
  (. frame show))
;  (require 'zutil.repl))
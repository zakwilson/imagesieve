(ns imagesieve.ui
  (:use imagesieve.logic imagesieve.image zutil.util)
  (:import (javax.swing JFileChooser JFrame JPanel JLabel UIManager JButton JOptionPane WindowConstants)
           (java.awt.event ActionListener)
           (java.io File))
  (:gen-class))
 
(in-ns 'imagesieve.ui)
 
(when-not (= "com.sun.java.swing.plaf.gtk.GTKLookAndFeel"
             (UIManager/getSystemLookAndFeelClassName))
  (UIManager/setLookAndFeel (UIManager/getSystemLookAndFeelClassName)))
 
 
(def *count* (ref 0))
 
(def *max* (atom 0))
 
(def *status* (JLabel. "0/0 completed"))
 
(defn update-counter [& args]
  (dosync (alter *count* inc)
          (.setText *status*
                    (str @*count* "/" @*max* " completed"))))

 
(defn choose-and-go []) ;placeholder
 
(def frame (doto (JFrame. "Greyscale Converter")
             (.add (doto (JPanel.)
                     (.add (doto (JButton. "Convert a folder to greyscale")
                             (.addActionListener
                              (proxy [ActionListener] []
                                (actionPerformed [e] (choose-and-go))))))
                     (.add *status*)))
             (.setDefaultCloseOperation (WindowConstants/EXIT_ON_CLOSE))
             (.pack)))
 
(def chooser (doto (JFileChooser.)
               (.setFileSelectionMode (JFileChooser/DIRECTORIES_ONLY))))
 
(defn process-and-update [img]
  (make-grey! img)
  (update-counter)
  img)

(defn do-processing [state path]
 (doall (process-dir process-and-update
                     path
                     (str path "-grey/")))
 nil)

(def *processor* (agent nil))

(defn error->str [e]
  (str (.getMessage e) ", "))
 
(defn choose-and-go []
  (let [status (.showOpenDialog chooser frame)]
    (when (= (JFileChooser/APPROVE_OPTION) status)
      (try
       (let [path (.getAbsolutePath (.getSelectedFile chooser))
             dir (File. path)]
         (if (and (.exists dir) (.isDirectory dir))
           (do (swap! *max* identity* (count (.list dir)))
               (dosync (ref-set  *count* -1))
               (update-counter)
               (send-off *processor* do-processing path))
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

(ns logkeeper.logstash
  (:import [java.io File Reader])
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:use [logkeeper.render :only [render-tpl]]
        logkeeper.util
        environ.core
        clojure.tools.logging))

(defn get-module-path [module]
  (-> (env :workdir)
      (str separator module separator)))


(defn param-name-file [req parent-path]
  (let [name (-> req :params :name)
        path (str parent-path name)]
    (File. path)))

(defonce template-path (get-module-path "template"))
(defonce definition-path (get-module-path "definition"))

;;;definition begin

(def ^:dynamic module)
(def ^:dynamic type)
(def ^:dynamic ip)
(def ^:dynamic path)

(defrecord definition-record [module ip type path])

(defn definition-list []
  (listfiles definition-path #"^\."))

(defn definition-name-list []
  (->> definition-list
       (map #(.getName %))))

(defn parse-definition [content-lines]
  (binding [module nil type nil ip nil path nil]
    (loop [ret {} lines content-lines]
      (if (empty? lines)
        ret
        (let [line (first lines)]
          (if (normal-line? line)
            (let [items (-> (.split splitter line) reverse)
                  icount (count items)]
              (when (> icount 0) (set! path (first items)))
              (when (> icount 1) (set! ip (second items)))
              (when (> icount 2) (set! type (nth items 2)))
              (when (> icount 3) (set! module (nth items 3)))
              (let [record (definition-record. module ip type path)
                    already-records (or (:definitions ret) ())
                    new-records (conj already-records record)]
                (recur (conj ret {:definitions new-records}) (rest lines))))
            (if (meta-line? line)
              (recur (conj ret (read-string line)) (rest lines))
              (recur ret (rest lines)))))))))

(defn aggregate-definition [parse-ret]
  (let [definitions (:definitions parse-ret)]
    (loop [ret {} left definitions]
      (if (empty? left)
        (assoc parse-ret :definitions ret)
        (let [definition (first left)
              key (keyword (str (:ip definition) "-" (:module definition)))
              logs (key ret)]
          (if (nil? logs)
            (recur (conj ret {key (list definition)}) (rest left))
            (if (.contains logs definition)
              (recur ret (rest left))
              (recur (conj ret {key (conj logs definition)}) (rest left)))))))))

(def load-definition (comp aggregate-definition parse-definition))

(defn check-meta-line [meta-line]
  (let [meta-info (read-string meta-line)
        template (:template meta-info)
        es (:es meta-info)]
    (if (or (nil? template) (nil? es))
      (throw (Exception. "template or es meta info is required!"))
      (let [template-file (File. (str template-path template))]
        (when-not (.exists template-file)
          (throw (Exception. "template not exists!")))))))  ;;TODO：es合法性校验

(defn check-first-normal-line [normal-line]
  (let [items (-> (.split splitter normal-line) seq)
        icount (count items)]
    (if (not= 4 icount)
      (throw (Exception. "first normal line must contain four items!")))))

(defn get-definition-keys
  ([] (get-definition-keys (definition-list)))
  ([files] (loop [keyset #{} fs files]
             (if (empty? fs)
               keyset
               (let [rdr (io/reader (first fs))
                     lines (doall (line-seq rdr))
                     round-keys (-> lines load-definition :definitions keys)]
                 (.close rdr)
                 (recur (into keyset round-keys) (rest fs)))))))


(defn check-definition [content]
  (let [content-lines (string/split-lines content)]
    (loop [lines content-lines first-line (first content-lines)]
      (if-not (meta-line? first-line)
        (recur (rest lines) (first (rest lines)))
        (check-meta-line first-line)))
    (loop [lines content-lines first-line (first content-lines)]
      (if (normal-line? (first lines))
        (check-first-normal-line first-line)
        (recur (rest lines) (first (rest lines)))))
    (let [already-keys (get-definition-keys)
          round-keys (-> content-lines load-definition :definitions keys)]
      (doseq [key round-keys]
        (when (contains? already-keys key)
          (throw (Exception. (str (name key) " already defined in other definition files!"))))))))

(defn definition-home []
  (let [files (definition-name-list)]
    (render-tpl "logstash-definition.html" {:definition-list files})))

(defn definition-detail [req]
  (let [^File file (param-name-file req definition-path)
        file-name (.getName file)]
    (when-not (or (.exists file)
                  (.isDirectory file))
      (-> (str file-name " not exists!") Exception. throw))
    (with-open [rdr (io/reader file)]
      (let [lines (doall (line-seq rdr))]
        (render-tpl "definition-detail.html" {:lines lines :name file-name})))))

(defn delete-definition [req]
  (let [^File file (param-name-file req definition-path)
        file-name (.getName file)]
    (if (and (.exists file)
             (.isFile file))
      {:status 200 :body "200"}
      (throw (Exception. (str file-name " not exists or not a file"))))))

(defn new-definition [req]
  (let [name (-> req :params :name)
        path (str definition-path name ".conf")
        file (File. path)
        content (-> req :params :content)]
    (if (.exists file)
      {:status 400 :body (str "a file named " name " already exists")}
      (try
        (check-definition content)
        (.createNewFile file)
        (with-open [w (io/writer file)]
          (.write w content)
          (.flush w))
        "create definition success"
        ;start logstash process
        (catch Exception e
               (when (.exists file) (.delete file))
               {:status 500 :body (str "create definition error: " (.getMessage e))})))))

;;;definition end
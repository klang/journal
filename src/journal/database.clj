(ns journal.database
  (:use clojure.contrib.sql))

(defn create-article-table []
  (create-table
   :article
   [:id           :int "IDENTITY" "PRIMARY KEY"]
   [:title        :varchar "NOT NULL"] ; no default to empty
   [:description  :varchar "NOT NULL"]
   [:body         :varchar "NOT NULL"]
   [:created      :datetime ] ; no default to 'now'
   [:updated      :datetime ]
   [:published    :datetime ]))

(def db {:classname "org.hsqldb.jdbcDriver"
         :subprotocol "hsqldb"
         :subname "/tmp/articles.db"})

(try 
 (with-connection db (create-article-table)) 
 (catch Exception e 
   (println "database already exist")))

(defn drop-article-table []
  (try
   (drop-table :article)
   (catch Exception e)))

(defn now [] (java.sql.Timestamp. (.getTime (java.util.Date.))))

(defn insert-samples []
  (let [timestamp (now)]
    (seq
     (insert-values
      :article
      [:title :description :body :created :updated :published]
      ["Article 1" "My first article" 
       "<p>Paragraph 1 in article 1</p><p>Another paragraph</p>"
       timestamp timestamp nil]
      ["Article 2" "My second article" 
       "<p>Paragraph 1 in article 2</p><p>Another paragraph</p>"
       timestamp timestamp nil]))))

(defn sample-articles []
  (with-connection db
    (drop-article-table)
    (create-article-table)
    (insert-samples)))

(defn reset-article-table []
  (with-connection db
     (drop-article-table)
     (create-article-table)))

(defn ensure-article-table-exists []
  (try
   (with-connection db (create-article-table))
   (catch Exception _)))
 
(defmulti coerce (fn [dest-class src-inst] [dest-class (class src-inst)]))
(defmethod coerce [Integer String] [_ inst] (Integer/parseInt inst))
(defmethod coerce :default [dest-cls obj] (cast dest-cls obj))

(defn select-articles []
  (with-connection db
    (with-query-results res ["select * from article"] (doall res))))

(defn print-articles [] 
  (apply println (select-articles)))

(defn sql-query [q]
  (with-query-results res q (doall res)))

(defn select-articles [] 
  (with-connection db
    (sql-query ["select * from article"])))

(defn select-article [id]
  (with-connection db
    (first (sql-query ["select * from article where id = ?" (coerce Integer id)]))))

(comment
  ;; these two functions should exist ..
  ;; to avoid returning all the data every time.
  (defn select-article [id]
    (with-connection db
      (first (sql-query ["select * from article where id = ?" (coerce Integer id)]))))

  (defn select-article [title]
    (with-connection db
      (first (sql-query ["select * from article where title = ?" title]))))
  )

(defn last-created-id 
  "Extract the last created id. Must be called in a transaction
   that performed an insert. Expects HSQLDB return structure of
   the form [{:@p0 id}]."
  []
  (first (vals (first (sql-query ["CALL IDENTITY()"])))))

(defn insert-article [title description body]
  (let [timestamp (now)]
    (with-connection db
      (transaction
	(insert-values 
	 :article
	 [:title :description :body :created :updated :published]
	 [title description body timestamp timestamp nil])
	(last-created-id)))))

;; functions to insert sample data
(defn insert-more-samples []
  (let [timestamp (now)]
    (insert-article 
     "Article 3" "My third article" 
      "<p>Paragraph 1 in article 3</p><p>Another paragraph</p>")
    (insert-article        
     "Article 4" "My fourth article" 
     "<p>Paragraph 1 in article 4</p><p>Another paragraph</p>")))

(defn sample-more-articles []
  (with-connection db
    (drop-article-table)
    (create-article-table)
    (insert-samples)
    (insert-more-samples)))

;; populate the database with sample data

(with-connection db (drop-article-table))
(with-connection db (create-article-table))
(sample-more-articles)

(ns clojure-lsp.features.hover-test
  (:require
   [clojure-lsp.feature.hover :as f.hover]
   [clojure-lsp.test-helper :as h]
   [clojure.string :as string]
   [clojure.test :refer [deftest is testing]]))

(defn ^:private join [coll]
  (string/join "\n" coll))

(defn ^:private hover
  ([components row col]
   (hover components row col {}))
  ([components row col opts]
   (f.hover/hover h/default-uri row col components opts)))

(def ^:private capabilities-markdown {:client-capabilities {:text-document {:hover {:content-format ["markdown"]}}}})
(def ^:private settings-one-line {:settings {:hover {:arity-on-same-line? true}}})
(def ^:private settings-one-line-deprecated {:settings {:show-docs-arity-on-same-line? true}})
(def ^:private settings-hide-file {:settings {:hover {:hide-file-location? true}}})
(def ^:private settings-no-clojuredocs {:settings {:hover {:clojuredocs false}}})
(def ^:private settings-edits-warning {:settings {:completion {:additional-edits-warning-text "* includes additional edits"}}})

(deftest test-hover
  (let [components (h/make-components)]
    (h/with-db
      components
      settings-no-clojuredocs
      (let [start-code "```clojure"
            end-code "```"
            line-break "\n----\n"
            code (h/code "(ns a)"
                         "(defn foo \"Some cool docs :foo\" [x] x)"
                         "(defn bar [y] y)"
                         "(|foo 1)"
                         "(|bar 1)")
            [[foo-row foo-col]
             [bar-row bar-col]] (h/load-code code h/default-uri components)]
        (testing "with docs"
          (testing "show-docs-arity-on-same-line? disabled"
            (testing "plain"
              (is (= [{:language "clojure" :value "a/foo"}
                      {:language "clojure" :value "[x]"}
                      "Some cool docs :foo"
                      (h/file-path "/a.clj")]
                     (:contents (hover components foo-row foo-col))))
              (is (= [{:language "clojure" :value "a/foo"}
                      {:language "clojure" :value "[x]"}
                      "Some cool docs :foo"
                      (h/file-path "/a.clj")]
                     (:contents (hover components foo-row foo-col {:additional-text-edits? true}))))
              (h/with-db
                components
                settings-edits-warning
                (is (= [{:language "clojure" :value "a/foo"}
                        {:language "clojure" :value "[x]"}
                        "* includes additional edits"
                        "Some cool docs :foo"
                        (h/file-path "/a.clj")]
                       (:contents (hover components foo-row foo-col {:additional-text-edits? true}))))))
            (testing "markdown"
              (h/with-db
                components
                capabilities-markdown
                (is (= {:kind  "markdown"
                        :value (join [start-code
                                      "a/foo"
                                      "[x]"
                                      end-code
                                      ""
                                      "Some cool docs :foo"
                                      line-break
                                      (str "*[" (h/file-path "/a.clj") "](file:///a.clj)*")])}
                       (:contents (hover components foo-row foo-col))))
                (is (= {:kind  "markdown"
                        :value (join [start-code
                                      "a/foo"
                                      "[x]"
                                      end-code
                                      ""
                                      "Some cool docs :foo"
                                      line-break
                                      (str "*[" (h/file-path "/a.clj") "](file:///a.clj)*")])}
                       (:contents (hover components foo-row foo-col {:additional-text-edits? true}))))
                (h/with-db
                  components
                  settings-edits-warning
                  (is (= {:kind  "markdown"
                          :value (join [start-code
                                        "a/foo"
                                        "[x]"
                                        end-code
                                        ""
                                        "* includes additional edits"
                                        ""
                                        "Some cool docs :foo"
                                        line-break
                                        (str "*[" (h/file-path "/a.clj") "](file:///a.clj)*")])}
                         (:contents (hover components foo-row foo-col {:additional-text-edits? true}))))))))

          (testing "show-docs-arity-on-same-line? enabled"
            (h/with-db
              components
              settings-one-line-deprecated
              (testing "plain"
                (is (= [{:language "clojure" :value "a/foo [x]"}
                        "Some cool docs :foo"
                        (h/file-path "/a.clj")]
                       (:contents (hover components foo-row foo-col)))))

              (testing "markdown"
                (h/with-db
                  components
                  capabilities-markdown
                  (is (= {:kind  "markdown"
                          :value (join [start-code
                                        "a/foo [x]"
                                        end-code
                                        ""
                                        "Some cool docs :foo"
                                        line-break
                                        (str "*[" (h/file-path "/a.clj") "](file:///a.clj)*")])}
                         (:contents (hover components foo-row foo-col))))))))
          (testing "hover arity-on-same-line? enabled"
            (h/with-db
              components
              settings-one-line
              (testing "plain"
                (is (= [{:language "clojure" :value "a/foo [x]"}
                        "Some cool docs :foo"
                        (h/file-path "/a.clj")]
                       (:contents (hover components foo-row foo-col)))))

              (testing "markdown"
                (h/with-db
                  components
                  capabilities-markdown
                  (is (= {:kind  "markdown"
                          :value (join [start-code
                                        "a/foo [x]"
                                        end-code
                                        ""
                                        "Some cool docs :foo"
                                        line-break
                                        (str "*[" (h/file-path "/a.clj") "](file:///a.clj)*")])}
                         (:contents (hover components foo-row foo-col))))))))

          (testing "hide-filename? enabled"
            (h/with-db
              components
              settings-hide-file
              (testing "plain"
                (is (= [{:language "clojure" :value "a/foo"}
                        {:language "clojure" :value "[x]"}
                        "Some cool docs :foo"]
                       (:contents (hover components foo-row foo-col)))))
              (testing "markdown"
                (h/with-db
                  components
                  capabilities-markdown
                  (is (= {:kind  "markdown"
                          :value (join [start-code
                                        "a/foo"
                                        "[x]"
                                        end-code
                                        ""
                                        "Some cool docs :foo"])}
                         (:contents (hover components foo-row foo-col)))))))))
        (testing "without docs"
          (testing "show-docs-arity-on-same-line? disabled"
            (testing "plain"
              (is (= [{:language "clojure" :value "a/bar"}
                      {:language "clojure" :value "[y]"}
                      (h/file-path "/a.clj")]
                     (:contents (hover components bar-row bar-col))))
              (is (= [{:language "clojure" :value "a/bar"}
                      {:language "clojure" :value "[y]"}
                      (h/file-path "/a.clj")]
                     (:contents (hover components bar-row bar-col {:additional-text-edits? true}))))
              (h/with-db
                components
                settings-edits-warning
                (is (= [{:language "clojure" :value "a/bar"}
                        {:language "clojure" :value "[y]"}
                        "* includes additional edits"
                        (h/file-path "/a.clj")]
                       (:contents (hover components bar-row bar-col {:additional-text-edits? true}))))))
            (testing "markdown"
              (h/with-db
                components
                capabilities-markdown
                (is (= {:kind  "markdown"
                        :value (join [start-code
                                      "a/bar"
                                      "[y]"
                                      end-code
                                      line-break
                                      (str "*[" (h/file-path "/a.clj") "](file:///a.clj)*")])}
                       (:contents (hover components bar-row bar-col))))
                (is (= {:kind  "markdown"
                        :value (join [start-code
                                      "a/bar"
                                      "[y]"
                                      end-code
                                      line-break
                                      (str "*[" (h/file-path "/a.clj") "](file:///a.clj)*")])}
                       (:contents (hover components bar-row bar-col {:additional-text-edits? true}))))
                (h/with-db
                  components
                  settings-edits-warning
                  (is (= {:kind  "markdown"
                          :value (join [start-code
                                        "a/bar"
                                        "[y]"
                                        end-code
                                        ""
                                        "* includes additional edits"
                                        line-break
                                        (str "*[" (h/file-path "/a.clj") "](file:///a.clj)*")])}
                         (:contents (hover components bar-row bar-col {:additional-text-edits? true}))))))))

          (testing "show-docs-arity-on-same-line? enabled"
            (h/with-db
              components
              settings-one-line-deprecated
              (testing "plain"
                (is (= [{:language "clojure" :value "a/bar [y]"}
                        (h/file-path "/a.clj")]
                       (:contents (hover components bar-row bar-col)))))

              (testing "markdown"
                (h/with-db
                  components
                  capabilities-markdown
                  (is (= {:kind "markdown"
                          :value (join [start-code
                                        "a/bar [y]"
                                        end-code
                                        line-break
                                        (str "*[" (h/file-path "/a.clj") "](file:///a.clj)*")])}
                         (:contents (hover components bar-row bar-col)))))))))
        (testing "On function usage corner cases"
          (h/with-db
            components
            settings-one-line
            (let [code (h/code "(ns a)"
                               "(defn foo \"Some cool docs :foo\" [x y] x)"
                               "(defn bar \"Other cool docs :bar\" [x y] x)"
                               "(foo"
                               "  1"
                               "  |2)"
                               "(->> :foo foo |bar)"
                               "(map #(foo %1 |%2) [1 2 3])")
                  [[foo-row foo-col]
                   [bar-row bar-col]
                   [anon-row anon-col]] (h/load-code code h/default-uri components)]
              (is (= [{:language "clojure"
                       :value "a/foo [x y]"}
                      "Some cool docs :foo"
                      "/a.clj"]
                     (:contents (hover components foo-row foo-col))))
              (is (= [{:language "clojure"
                       :value "a/bar [x y]"}
                      "Other cool docs :bar"
                      "/a.clj"]
                     (:contents (hover components bar-row bar-col))))
              (is (= [{:language "clojure"
                       :value "a/foo [x y]"}
                      "Some cool docs :foo"
                      "/a.clj"]
                     (:contents (hover components anon-row anon-col)))))))
        (testing "On function definition"
          (h/with-db
            components
            settings-one-line
            (let [code (h/code "(ns a)"
                               "(defn |foo \"Some cool docs :foo\" [x y] x)")
                  [[foo-row foo-col]] (h/load-code code h/default-uri components)]
              (is (= [{:language "clojure"
                       :value "a/foo [x y]"}
                      "Some cool docs :foo"
                      "/a.clj"]
                     (:contents (hover components foo-row foo-col)))))))
        (testing "on a require with docs"
          (h/load-code (h/code "(ns ^{:doc \"Some cool docstring\"} some-a)") (h/file-uri "file:///some_a.clj") components)
          (let [code-b (h/code "(ns some-b (:require [some-|a :as abc]))")
                [[row col]] (h/load-code code-b (h/file-uri "file:///some_b.clj") components)]
            (is (= [{:language "clojure"
                     :value "some-a"}
                    "Some cool docstring"
                    "/some_a.clj"]
                   (:contents (f.hover/hover (h/file-uri "file:///some_b.clj") row col components))))))))))

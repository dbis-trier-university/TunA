# Evaluation
In this section, we present the evaluation of TunA on a set of simulated APIs. The tool used to simulate APIs ([ETARA](https://github.com/ETARA-Benchmark-System)) is able to cover all characteristics, i.e., latency, timeouts, rate limits, and configurable response structures, e.g., JSON or XML. It was developed to ensure reproducibility and hence contains open data. The response times for each simulated API were set between 100 and 1000 milliseconds. Moreover, the APIs were set to allow only two requests per second to replicate the settings of many real APIs. Note that we performed some experiments on real world APIs. However, for the sake of shortness those results can be found on GitHub.

As [data sources](#) we used the ones provided by ETARA which consists of two domains, namely bibliographic data and filmographic data. The bibliographic set contains an RDF version of dblp and data exports of the RESTful Web APIs CrossRef, SciGraph and Semantic Scholar. The filmographic set contains the Linked Movie Database (LMDB) and exports of Open Movie Database (OMDB), The Movie Database (TMDB) and Internet Movie Database (IMDB). We used dblp and LMDB in our experiments as local KBs. To ensure both datasets contain missing information, we removed randomly chosen triples from both KBs, such as triples containing titles and author names. Additionally, we created three *tainted* data sets that contain erroneous data, e.g., incorrect titles.

We created four experiments to evaluate the performance of TunA. The first experiment is used to compare TunA's computed execution plan with the optimum execution plan. The second experiment focuses on the tunability and scalability of TunA. The third experiment compares the performance of the state of the art system ANGIE with TunA. ANGIE is a framework for generating queries to encapsulated RESTful Web APIs and efficient algorithms for query execution and result integration. TunA was implemented in Java using the Apache Jena framework. To make a comparison with ANGIE as fair as possible, ANGIE was likewise re-implemented in Java. Lastly, we show that TunA can be used with real Web APIs, e.g., CrossRef, Springer Nature, Semantic Scholar and arXiv. 

## First Experiment: Comparison with Optimal Plans
The aim of the first experiment is to analyze TunA's computed plans compared to the optimal plans. As explained before, finding the best combination of API calls is NP-hard (as we can reduce the knapsack problem to it). For a result table with ten gaps and for each gap three available APIs that can provide missing titles, there are (2^3)^10 combinations. To determine the optimal plan, we have to find the combination that satisfies all user preferences and requires the fewest calls. For higher numbers of gaps it is thus not possible to enumerate all plans and select the best one. Hence, we have created a set of 20 queries with less than ten gaps in the result tables. Moreover, we evaluated all combinations of coverage (1.0 to 0.7) and reliability (0.9 to 0.7) resulting in a total of *240 TunA queries*.

| ![opt_calls](https://github.com/dbis-trier-university/TunA/assets/4719393/a16a960f-905b-43cf-87ec-c1550e68889a) |
|:--:| 
| **Figure 1:** Number of Calls |


| ![opt_time](https://github.com/dbis-trier-university/TunA/assets/4719393/b822a9f6-184a-4f3c-a120-755a97bacda6) |
|:--:| 
| **Figure 2:** Execution Time |

As shown in Figure 1 and 2, TunA is close in terms of execution time and number of calls to the optimal plan. Only combinations of high coverage and reliability (around 0.9) result in a higher number of calls, but this subsequently levels off again for lower values. However, even if TunA uses more calls, it is close to the optimal execution time and in some cases can even beat the execution time of an optimal plan. This is because TunA selects and combines APIs according to the round robin principle which allows more calls to be executed in parallel in contrast to the optimal plan. Additionally, the computation of the optimal plan took longer than fifteen minutes, whereas TunA's approach took less than 60 milliseconds to generate a plan for a table with 180 gaps.

## Second Experiment: Tunability
To evaluate the performance and tunability of TunA for different combinations of coverage (1.0 to 0.7) and reliability (0.9 to 0.7) we used ten queries with a similar number of gaps, resulting in *360 TunA queries*. We used for both systems three function sets. The first set (**F1**) consists of two trusted APIs (i.e., reliability of about 0.9) and one untrusted API (i.e., reliability of about 0.65). The second set (**F2**) consists of two trusted and untrusted APIs and the last set (**F3**) consists of three untrusted APIs. 

| ![coverage](https://github.com/dbis-trier-university/TunA/assets/4719393/0af3d15a-2a2e-43d4-9d48-8bd1b93ae07e) |
|:--:| 
| **Figure 3:** Coverage |

| ![reliability](https://github.com/dbis-trier-university/TunA/assets/4719393/2652e159-0af0-49db-926b-aa78012b4489) |
|:--:| 
| **Figure 4:** Erroneous Data |

| ![time](https://github.com/dbis-trier-university/TunA/assets/4719393/dc21f2fc-cc9d-4e08-8f4b-d1af319c13c8) |
|:--:| 
| **Figure 5:** Execution Time |

| ![calls](https://github.com/dbis-trier-university/TunA/assets/4719393/956d599e-0bbf-4c8e-a0ed-fe556da237da) |
|:--:| 
| **Figure 6:** API Calls |

Figure 3 shows whether TunA and ANGIE could achieve the required coverage using F1, F2, and F3. Since ANGIE is designed to request APIs until all information are provided it returns always a complete coverage. For TunA the desired coverage of 0.7 to 0.9 can always be achieved and even a requested coverage of 1.0 is in most cases achieved. Only for F3 the required coverage could not be achieved because no majority could be obtained during the voting phase. For reliability reasons, these results have been removed. When it comes to reliability, TuNA was able to consistently meet user preferences (thresholds). As shown in Figure 4 ANGIE includes much erroneous information in contrast to TuNA, since APIs that deliver a result quickly and reliably are requested first.

Figure 5 and 6 show the required execution time and the number of calls. High coverage and reliability values require more time and calls than lower values. An increase in coverage from 0.9 to 1.0 leads to significantly more calls than the increase from 0.8 to 0.9. ANGIE needs significantly less calls than TunA because it does not check for correctness. Furthermore, it is noticeable that with a coverage of 1.0, the plan created using F2 is executed faster than the plans using F1 and F3 because F2 contains one API more than F1 and F3 and is hence better parallelizable. However, the increasing runtime between F1 and F3 clearly shows that with increasing number of untrusted APIs, the runtime increases. 

## Third Experiment: Comparison with Base Line
Figure 7 and 8 present the results of our last experiment. Since ANGIE is designed to retrieve a full coverage we executed TunA for this experiment with required coverage of 1.0 and reliability of 0.9. We executed a total of 45 queries against dblp and LMDB using a set of nine different (two untrusted) function definitions. The aim was to evaluate how TunA and ANGIE perform for different queries and especially how both processors handle the increasing amount of results for each query. Note that since we have removed information from dblp also the amount of missing information increases with each query.

| ![comp_reliability](https://github.com/dbis-trier-university/TunA/assets/4719393/ccca7640-4616-4618-93ac-4099b642f393) |
|:--:| 
| **Figure 7:** Reliability Comparison |

| ![comp_time](https://github.com/dbis-trier-university/TunA/assets/4719393/e74a5c49-8b28-495a-a51a-c6f3511c3cba) |
|:--:| 
| **Figure 8:** Execution Time Comparison |

Both ANGIE and TunA were able to deliver the required coverage. However, Figure 7 shows that the reliability of TunA, in contrast to ANGIE, is always above the required threshold of 0.9. ANGIE was only able to achieve the required reliability for a few queries (3, 6 and 10). In all other cases, ANGIE integrated incorrect information into the result table because it does not take reliability into account during the retrieval and the result integration.

Figure 8 shows the execution time of the computed plans. Since the queries are built in such a way that the result tables get increasingly larger, it is no surprise that the runtime increases. Figure 8 shows that for smaller queries the runtime of both systems is nearly the same. Only for queries with large result tables ANGIE can show its strength in terms of execution time. A disadvantage of this approach is the significantly larger amount of erroneous data in the results table and that in most cases the desired reliability can not be achieved. If the results from the first experiment are taken into account, it is clear that TunA cannot be executed much faster, since this results in more erroneous data.

## Fourth Experiment: Using Real World Web APIs
Figure 9 and 10 present the results of our last experiment. We used the same configurations as in the third experiment to retrieve a full coverage with a reliability of 0.9. We executed a total of 25 queries against dblp using as function definitions the Web APIs of CrossRef, Springer Nature and Semantic Scholar. The aim was to evaluate how TunA performs for different queries in a real world szenario. As in the previous expermiment, the amount of missing information increases with each query.

| ![real_time](https://github.com/dbis-trier-university/TunA/assets/4719393/ac2c24a7-1da1-4f34-979f-13539741a29f) |
|:--:| 
| **Figure 9:** Execution Time |

| ![real_calls](https://github.com/dbis-trier-university/TunA/assets/4719393/3ae51afc-17a9-4623-96ae-1ba8c5b1b7f9) |
|:--:| 
| **Figure 10:** Number of Calls |

Figure 9 shows the execution time of the computed plans im comparison to the number of gaps contained in the query result. Not very surprisingly, the runtime tends to increase with the number of gaps. However, it is important to note that the main reason for the high runtime, especially for the later queries, is not only determined by the number of gaps, but also by the user preferences. As shown in the previous experiments, a coverage or reliability of more than 0.9 leads to an increased number of necessary queries.

Figure 10 clearly shows that for almost all queries the number of calls is almost three times higher than the number of gaps. This is due to the fact that each of the three Web APIs must be queried due to the required coverage of 1.0. All Web APIs have a response probability greater than 0.9 but less than 1.0, so it is quite realistic that a gap in the result table will not be filled. This would mean that the required coverage would not be met.

TunA was for all queries able to achieve the required coverage and reliability values. The reason for this is mainly due to two aspects. Firstly, the selected APIs are very high quality in terms of their data and secondly, as already mentioned, all three web APIs are requested for each gap. This makes it almost impossible for there to be a loss in coverage or reliability.


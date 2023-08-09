# TunA: Tunable Query Optimizer for Web APIs and User Preferences

## What is TunA?
To answer queries many query processors combine different SPARQL sources, e.g., various knowledge bases or APIs. RESTful Web APIs are rarely the focus of those systems as they come with many limitations, such as not being able to process SPARQL queries. Additionally, most existing approaches optimize their query plans only for performance, even though users often have additional preferences regarding coverage, reliability, or currency. In addition, data is often provided with different levels of quality so that not all sources should be trusted equally.

TunA is a query optimizer that is able to combine RESTful Web APIs and RDF KBs in form of triple stores, tuning its (query) plans towards user preferences. Erroneous information from Web APIs is detected using a hierarchical agglomerative clustering. Our evaluation shows that TunA outperforms current state-of-the-art systems and is less vulnerable for erroneous information, even in settings where only unreliable sources are available

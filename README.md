Ensemble Bandit
------------------------

This repository contains the code and data needed to reproduce the experiments of the paper: 
  
> R. Cañamares, M. Redondo, [P. Castells](http://ir.ii.uam.es/castells/). [Multi-Armed Recommender System Bandit Ensembles](http://ir.ii.uam.es/pubs/recsys2019-rcanamares.pdf). 13th ACM Conference on Recommender Systems (RecSys 2019). Copenhagen, Denmark, September 2019.

The software produces the results displayed in figures 1, 2 and 3 in the paper.

Authors
--------------------
Information Retrieval Group at Universidad Autónoma de Madrid
- Rocío Cañamares (rocio.cannamares@uam.es)
- Pablo Castells (pablo.castells@uam.es)
  
Software description
--------------------
  
This repository contains all the needed classes to reproduce the experiments reported in the paper. The software contains the following packages:
- `es.uam.ir.ensemblebandit.arm`: classes implementing bandit arms.
- `es.uam.ir.ensemblebandit.bandit`: classes implementing different bandit strategies.
- `es.uam.ir.ensemblebandit.ensemble`: class implementing a dynamic ensemble.
- `es.uam.ir.ensemblebandit.datagenerator`: classes to handle the data and generate the different training sets for each algorithm.
- `es.uam.ir.ensemblebandit.filler`: classes to complete recommendation rankings when an algorithm falls short of coverage.
- `es.uam.ir.ensemblebandit.util`: additional classes, useful for the rest of the program.
- `es.uam.ir.ensemblebandit`: top-level main classes to generate the figures of the paper.
  
The software uses the [RankSys](http://ranksys.org/) library, and extends some of its classes. Our extensions are located in the following package:
- `es.uam.ir.ensemblebandit.ranksys.rec.fast.basic`: extension of RankSys implementations of non-personalized recommendation, adding popularity-based recommendation.
  
  
Data
----
  
The repository includes for convenience a copy of the dataset [MovieLens 1M](https://grouplens.org/datasets/movielens/1m) used in the paper, that is needed for the reproduction of the experiments.

System Requirements
-------------------

- Java JDK:
    1.8 or above (the software was tested using the version 1.8.0_181).

- Maven:
    tested with version 3.6.0.

	
Installation
------------
  
  Download all the files and unzip them into any root folder.
  
  From the root folder run the command: 
  
    mvn compile assembly::single
    
  
Execution
---------
  
  To generate the experiments of figures 1 and 2 of the paper, run the command:
  
  	java -cp .\target\EnsembleBandit-0.1-jar-with-dependencies.jar es.uam.ir.ensemblebandit.Figure1and2
	
  Three files will be generated inside the root folder: `figure1.txt`, `figure2-epsilon-greedy.txt` and `figure2-thompson-sampling.txt`. 
- `figure1.txt` contains the recall reached by each algorithm in each epoch. 
- `figure2-epsilon-greedy.txt` and `figure2-thompson-sampling.txt` contain the number of times each arm has been selected by epsilon-greedy bandit and Thompson sampling bandit, respectively, in each epoch.
	
To generate the experiment of figure 3 run the command:
		
	java -cp .\target\EnsembleBandit-0.1-jar-with-dependencies.jar es.uam.ir.ensemblebandit.Figure3
  
  A file `figure3.txt` will be generated inside the root folder, with the recall reached by each algorithm in each epoch. 
  
    
Example of the output files
---------------------------
  
  Exact values change slightly from one execution to another:
  
  
- `figure1.txt`:

		Epoch	Random recommendation	Most popular	User-based kNN	Matrix factorization	Thompson sampling bandit	Epsilon-greedy bandit	Dynamic ensemble
		0	2.696704554150921E-4	0.004934240495027496	3.1704499487990556E-4	4.701011993046875E-4	0.0020225284156131906	0.0018548954298146197	4.701011993046875E-4
		1	5.959866586472744E-4	0.009523722327003718	7.746371052996114E-4	6.362111710660639E-4	0.006815675505926515	0.006465863453815261	6.963686743473822E-4
		2	8.769514905440563E-4	0.013965714832317606	0.0013621618073170153	8.91572723344437E-4	0.011313831787702835	0.010842215685267644	9.827947107775565E-4
		3	0.001121573047231903	0.017744897695232676	0.002100092871179099	0.0014261510881241007	0.015549588962334908	0.014987861618770101	0.001535714481130957
		4	0.001402765034120572	0.021789122685983674	0.0029178314559759853	0.0023794057324701295	0.019419667641289464	0.018824408477457134	0.002500109493846446
		5	0.0016987532136725002	0.025713642008754926	0.003859508058915975	0.0028620272723619855	0.02345613422399893	0.022774023466508243	0.0030158115473981092
		6	0.001963908616192755	0.02911732161014415	0.0048746492630408296	0.0035906741148028967	0.027161962808029228	0.02656038288997691	0.0036204668775711986
		7	0.002230977357953197	0.033234566049058584	0.006018460028600044	0.004246113672230014	0.030685778674563034	0.02989896495668457	0.004149316889676448
		8	0.0024616688215399673	0.03707239369153599	0.007231486386658137	0.00478686028808994	0.03480872387420552	0.03279251550667655	0.004784566434116612
		9	0.0027343964052824812	0.04087283063298364	0.008548781092107702	0.005393608957858082	0.03890360843697257	0.035747019356538595	0.005443031253947708
		10	0.002994586357914277	0.0451088884294307	0.009861070273448985	0.00601606237359674	0.042791581785273425	0.039347028447182464	0.0061830739495644365
		...
	
- `figure2-epsilon-greedy.txt` and `figure2-thompson-sampling.txt`:

		Epoch	Most popular	User-based kNN	Matrix factorization
		0	1893	1865	1987
		1	5637	194	202
		2	5630	219	189
		3	5596	215	228
		4	5605	221	211
		5	5622	205	204
		6	5692	169	177
		7	5644	205	188
		8	5654	188	192
		9	5608	219	209
		10	5655	184	197
		...


- `figure3.txt`:

		Epoch	Random recommendation	Most popular	User-based kNN	Matrix factorization	Dynamic ensemble
		0	2.847825895878563E-4	0.004953810725052957	2.7950883792882196E-4	4.6057431155566885E-4	0.004953810725052957
		1	5.433511578303232E-4	0.008540077768767767	6.541280402886603E-4	6.032434447838788E-4	0.008540077768767767
		2	7.914926848487194E-4	0.013169722403953045	0.001153948315077144	8.372734436564224E-4	0.013169722403953045
		3	0.0010555853660682085	0.01660946124785591	0.0017775462470014907	0.0013125045084353927	0.01660946124785591
		4	0.0013268690992600154	0.02065840973674334	0.0024707446574329262	0.0022460478534929037	0.02065840973674334
		5	0.0016335123507967774	0.02459016393442623	0.003290146126897077	0.002744992327059317	0.02459016393442623
		6	0.0019439118055665605	0.028534578880340663	0.004252294581190431	0.003291045078680127	0.028534578880340663
		7	0.0022263280910117286	0.032349994756009796	0.005315618137009131	0.0038713341375831586	0.032349994756009796
		8	0.0024806594889630034	0.03698057045365858	0.0064081266214716425	0.004461228955634772	0.03698057045365858
		9	0.002768540498055325	0.04122301633605601	0.007577459755249642	0.005164860584079516	0.04122301633605601
		10	0.0030355166019136797	0.04547043908868731	0.008787187462360868	0.005832876857561889	0.04547043908868731
		...

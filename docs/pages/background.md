---
layout: default
---

# Background

Botsing (Dutch for 'crash') is a complete re-implementation of the crash replication tool [EvoCrash](http://www.evocrash.org) ([github](https://github.com/STAMP-project/EvoCrash)).
Whereas EvoCrash was a full clone of EvoSuite (making it hard to update EvoCrash as EvoSuite evolves), Botsing relies on EvoSuite as a (maven) dependency only. Furthermore, it comes with an extensive test suite, making it easier to extend. The license adopted is Apache, in order to facilitate adoption in industry and academia.

The underlying evolutionary algorithm and fitness function are described in:

* Mozhan Soltani, Annibale Panichella, and Arie van Deursen. Search-Based Crash Reproduction and Its Impact on Debugging. _IEEE Transactions on Software Engineering_, 2018. ([DOI](http://dx.doi.org/10.1109/TSE.2018.2877664), [preprint](https://pure.tudelft.nl/portal/en/publications/searchbased-crash-reproduction-and-its-impact-on-debugging(1281ce36-7afc-43d9-ad83-b69c60fbd49a).html))

* Mozhan Soltani, Pouria Derakhshanfar, Annibale Panichella, Xavier Devroey, Andy Zaidman, and Arie van Deursen. Single-objective versus Multi-Objectivized Optimization for Evolutionary Crash Reproduction. In Colanzi and McMinn, editors, _Search-Based Software Engineering - 10th International Symposium, SSBSE 2018 - Proceedings_. Lecture Notes in Computer Science, Springer. 2018. p. 325-340. ([DOI](http://dx.doi.org/10.1007/978-3-319-99241-9_18), [preprint](https://pure.tudelft.nl/portal/en/publications/singleobjective-versus-multiobjectivized-optimization-for-evolutionary-crash-reproduction(ccece8a1-79cd-4303-adca-34a920bf7d14).html)).

## Funding

Botsing is partially funded by research project STAMP (European Commission - H2020) ICT-16-10 No.731529.

![STAMP - European Commission - H2020](../assets/logo_readme_md.png)

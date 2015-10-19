#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики

S -> Adj<gnc-agr[1]>+ interp(NGrams.adj)  Noun<gnc-agr[1], rt> interp (NGrams.noun::norm="nom,sg") ; // цепочка из нескольких прилагательных и существительного в нормализованной форме, прилагательные и существительное согласованы

S -> Adv interp (NGrams.adv) Verb interp (NGrams.verb) ; // цепочка из наречия и глагола

S -> Noun<gnc-agr[1]>+ interp (NGrams.noun::norm="nom,sg") Noun<gnc-agr[1]> interp (NGrams.noun::norm="nom,sg"); // цепочка из двух или более существительных, согласованных по роду, числу, падежу

S ->Noun<sp-agr[4]> interp (NGrams.noun::norm="nom,sg")  Verb<sp-agr[4]> interp (NGrams.verb) ; // цепочка из нормализованного существительного и глагола, согласованного с ним


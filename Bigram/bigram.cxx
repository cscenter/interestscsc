#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики


S -> Adj<gram="A"> interp(NGrams.Bigram_1::norm = "nom, sg, m") Noun<no_hom> interp(NGrams.Bigram_2::norm = "nom, sg");
S -> Noun<no_hom> interp(NGrams.Bigram_2::norm = "nom, sg")  Adj<gram="A"> interp(NGrams.Bigram_1::norm = "nom, sg, m");
S -> Adv interp(NGrams.Bigram_1)  Verb interp(NGrams.Bigram_2::norm = "inf");
S -> Verb interp(NGrams.Bigram_2::norm = "inf") Adv interp(NGrams.Bigram_1);
S -> Noun<no_hom> interp(NGrams.Bigram_1::norm = "nom, sg")  Noun<no_hom> interp(NGrams.Bigram_2::norm = "nom, sg");
S -> Noun<no_hom> interp(NGrams.Bigram_1::norm = "nom, sg")  Verb interp(NGrams.Bigram_2::norm = "inf");


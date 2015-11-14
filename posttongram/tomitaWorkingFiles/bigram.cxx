#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики


Bigrams -> Adj<gram="A">  Noun<no_hom> | Noun<no_hom>  Adj<gram="A">  | Adv  Verb | Verb Adv | Noun<no_hom>  Noun<no_hom> | Noun<no_hom>  Verb ;

S -> Bigrams interp(NGrams.Bigram);


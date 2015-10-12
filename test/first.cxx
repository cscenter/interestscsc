#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики
//S -> Adj interp (Word.Norm::norm="nom,sg");
//S -> Adv interp (Word.Norm);
//S -> Noun interp (Word.Norm::norm="nom,sg");
//S -> Verb interp (Word.Norm::norm="inf");
S -> Adj interp (Test.adj);
S -> Adv interp (Test.adv);
S -> Noun interp (Test.noun::norm="nom,sg");
S -> Verb interp (Test.verb);

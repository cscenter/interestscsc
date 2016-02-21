#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики

S -> Noun<no_hom,rt> interp(NGrams.Head) Word<GU=[A]>* Noun<GU=[gen], no_hom> interp(NGrams.Dependent);
S -> Noun<GU=[gen], no_hom> interp(NGrams.Dependent) Word<GU=[A]>* Noun<no_hom,rt> interp(NGrams.Head);

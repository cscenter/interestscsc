#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики


//SuperNoun -> Noun<GU=[gen], no_hom> interp(NGrams.Dependent) Word<GU=~[ADV]>*;
//S -> Noun<no_hom,rt> interp(NGrams.Head) Word<GU=[A]|[V]>* SuperNoun+;

S -> Noun<no_hom,rt> interp(NGrams.Head) Word<GU=[A]|[V]>* Noun<GU=[gen], no_hom> interp(NGrams.Dependent);
//S -> Noun<GU=[gen], no_hom> interp(NGrams.Dependent) Word<GU=[A]|[V]>* Noun<no_hom,rt> interp(NGrams.Head);

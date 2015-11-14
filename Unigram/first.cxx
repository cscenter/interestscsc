#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики
S -> Word<gram='A'> interp (Word.Norm::norm="nom,pl,brev,m");
//S -> Word<gram='V,brev'> interp (Word.Norm);
S -> Adv interp (Word.Norm);
S -> Word<gram='ger'> interp (Word.Norm::norm="inf");
S -> Noun<no_hom> interp (Word.Norm::norm="nom,sg");
S -> Verb interp (Word.Norm::norm="inf");
S -> Participle interp (Word.Norm::norm="m,sg,nom,plen");
S -> Word<gram='pass'> interp (Word.Norm::norm="m,brev");

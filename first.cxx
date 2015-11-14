#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики
S -> Word<gram='A'> interp (Word.Norm::norm="nom,sg");
S -> Word<gram='ADV'> interp (Word.Norm);
S -> Noun<no_hom> interp (Word.Norm::norm="nom,sg");
S -> Word<gram='V'> interp (Word.Norm::norm="inf");
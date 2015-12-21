#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики

S -> Word<GU=['A','nom']> interp (NGrams.Unigram);

S -> Word<GU=['S','nom'],no_hom> interp (NGrams.Unigram);

S -> Word<gram='inf'> interp (NGrams.Unigram);

S -> Word<gram='ADV'> interp (NGrams.Unigram);

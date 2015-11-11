#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики

Unigrams -> Adj<gram='A'> |  Noun<no_hom> | Adv | Verb;


S -> Unigrams interp(NGrams.Unigram);




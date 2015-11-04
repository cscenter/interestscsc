#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики

Trigrams ->  Adj<gram='A', gnc-agr[1]> Adj<gram='A', gnc-agr[1]>  Noun<gnc-agr[1], rt> ;

S -> Trigrams interp(NGrams.Trigram);



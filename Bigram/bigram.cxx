#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики


Bigrams -> Adj<gram='A', gnc-agr[1]>  Noun<gnc-agr[1], rt> | Noun<gnc-agr[1], rt>  Adj<gnc-agr[1], gram='A'>  | Adv  Verb | Verb  Adv | Noun<gnc-agr[1]>  Noun<gnc-agr[1]> | Noun<sp-agr[4]>  Verb<sp-agr[4]> ;

S -> Bigrams interp(NGrams.Bigram);


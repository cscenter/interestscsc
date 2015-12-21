#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики

S ->  Adj<gram='A'> interp(NGrams.Trigram_1::norm = "nom, sg, m") Adj<gram='A'> interp(NGrams.Trigram_2::norm = "nom, sg, m")  Noun interp(NGrams.Trigram_3::norm = "nom, sg");




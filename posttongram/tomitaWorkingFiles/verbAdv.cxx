#encoding "utf-8"    // сообщаем парсеру о том, в какой кодировке написана грамматика
#GRAMMAR_ROOT S      // указываем корневой нетерминал грамматики

//S -> Adv interp(NGrams.Dependent)  Verb interp(NGrams.Head::norm = "inf");
//S -> Verb interp(NGrams.Head::norm = "inf") Adv interp(NGrams.Dependent);

SuperAdv -> Adv interp(NGrams.Dependent) Word<GU=~[ADV]>*;
S -> SuperAdv+ Verb interp(NGrams.Head);

SuperAdv -> Adv interp(NGrams.Dependent) Word<GU=~[ADV]>*;
S -> Word<gram='V'> interp(NGrams.Head::norm="inf") SuperAdv+;

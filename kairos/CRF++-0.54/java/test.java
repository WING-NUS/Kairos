import org.chasen.crfpp.Tagger;

import java.io.File;
import java.io.IOException;


public class test {

public static void main(String[] argv) {
  Tagger tagger = new Tagger("-m CrfppPaperMetadataModel -v 1 -n 20");
  // clear internal context
  tagger.clear();

  // add context
  tagger.add("New new NEW tokenTextLength9 wordIsNumberOFF authorON NNP initialCapsON allCapsOFF onlyPunctiationOFF lineTextLength72 titleON inBOFF inTHOFF inTDON inIOFF inStrongOFF inFontOFF inEMOFF inAON affiliationON author");
  tagger.add("submillimeter submillimeter SUBMILLIMETER tokenTextLength9 wordIsNumberOFF authorOFF JJ initialCapsOFF allCapsOFF onlyPunctiationOFF lineTextLength72 titleOFF inBOFF inTHOFF inTDON inIOFF inStrongOFF inFontOFF inEMOFF inAON affiliationOFF author");
  tagger.add("airborne airborne AIRBORNE tokenTextLength9 wordIsNumberOFF authorOFF JJ initialCapsOFF allCapsOFF onlyPunctiationOFF lineTextLength72 titleOFF inBOFF inTHOFF inTDON inIOFF inStrongOFF inFontOFF inEMOFF inAON affiliationOFF title");
  tagger.add("instrument instrument INSTRUMENT tokenTextLength9 wordIsNumberOFF authorOFF NN initialCapsOFF allCapsOFF onlyPunctiationOFF lineTextLength72 titleOFF inBOFF inTHOFF inTDON inIOFF inStrongOFF inFontOFF inEMOFF inAON affiliationOFF title");
  tagger.add("for for FOR tokenTextLength9 wordIsNumberOFF authorON IN initialCapsOFF allCapsOFF onlyPunctiationOFF lineTextLength72 titleOFF inBOFF inTHOFF inTDON inIOFF inStrongOFF inFontOFF inEMOFF inAON affiliationON title");
  tagger.add("cloud cloud CLOUD tokenTextLength9 wordIsNumberOFF authorOFF NN initialCapsOFF allCapsOFF onlyPunctiationOFF lineTextLength72 titleOFF inBOFF inTHOFF inTDON inIOFF inStrongOFF inFontOFF inEMOFF inAON affiliationON title");
  tagger.add("and and AND tokenTextLength9 wordIsNumberOFF authorON CC initialCapsOFF allCapsOFF onlyPunctiationOFF lineTextLength72 titleOFF inBOFF inTHOFF inTDON inIOFF inStrongOFF inFontOFF inEMOFF inAON affiliationOFF title");
  tagger.add("surface surface SURFACE tokenTextLength9 wordIsNumberOFF authorOFF NN initialCapsOFF allCapsOFF onlyPunctiationOFF lineTextLength72 titleON inBOFF inTHOFF inTDON inIOFF inStrongOFF inFontOFF inEMOFF inAON affiliationOFF title");
  tagger.add("measurements measurements MEASUREMENTS tokenTextLength9 wordIsNumberOFF authorOFF NNS initialCapsOFF allCapsOFF onlyPunctiationOFF lineTextLength72 titleOFF inBOFF inTHOFF inTDON inIOFF inStrongOFF inFontOFF inEMOFF inAON affiliationOFF title");
  tagger.add("– – – tokenTextLength3 wordIsNumberON authorOFF NN initialCapsOFF allCapsOFF onlyPunctiationON lineTextLength11 titleOFF inBOFF inTHOFF inTDON inIOFF inStrongOFF inFontOFF inEMON inAON affiliationOFF title");
  tagger.add("Clare clare CLARE tokenTextLength3 wordIsNumberOFF authorON NNP initialCapsON allCapsOFF onlyPunctiationOFF lineTextLength11 titleOFF inBOFF inTHOFF inTDON inIOFF inStrongOFF inFontOFF inEMON inAON affiliationOFF title");
  tagger.add("Lee lee LEE tokenTextLength3 wordIsNumberOFF authorON NNP initialCapsON allCapsOFF onlyPunctiationOFF lineTextLength11 titleOFF inBOFF inTHOFF inTDON inIOFF inStrongOFF inFontOFF inEMON inAON affiliationON author");
		
  System.out.println("column size: " + tagger.xsize());
  System.out.println("token size: " + tagger.size());
  System.out.println("tag size: " + tagger.ysize());


  System.out.println("tagset information:");
  for (int i = 0; i < tagger.ysize(); ++i) {
    System.out.println("tag " + i + " " + tagger.yname(i));
  }
  

  // parse and change internal stated as 'parsed'
  if (!tagger.parse())
    return;
	
  	
  System.out.println("conditional prob=" + tagger.prob()
                     + " log(Z)=" + tagger.Z());

  for (int i = 0; i < tagger.size(); i++) {
    for (int j = 0; j < tagger.xsize(); j++) {
        System.out.print(tagger.x(i, j) + "\t");
    }
	System.out.print(tagger.x(i, 21) + "\t");
    System.out.print(tagger.y2(i) + "\t");
	System.out.print(tagger.result(i) + "\t");
	System.out.print("\n");
	

    System.out.print("Details");
    for (int j = 0; j < tagger.ysize(); j++) {
      System.out.print("\t" + tagger.yname(j) + "/prob=" + tagger.prob(i,j)
                       + "/alpha=" + tagger.alpha(i, j)
                       + "/beta=" + tagger.beta(i, j));
    }
    System.out.print("\n");
  }

  /*
  // when -n20 is specified, you can access nbest outputs
  System.out.println("nbest outputs:");
  for (int n = 0; n < 20; ++n) {
	  if (! tagger.next()) { 
		  break;
	  }
    System.out.println("nbest n=" + n + "\tconditional prob=" + tagger.prob());
    // you can access any information using tagger.y()...
  }
  System.out.println("Done");
  */
	
}

static {
	try {
		File dir = new File(".");
		
		try {
			System.load(dir.getCanonicalPath()
						+ "/libCRFPP.so");
		} catch (IOException e) {
			e.printStackTrace();
		}
	} catch (UnsatisfiedLinkError e) {
		System.err
		.println("Cannot load the CRFPP native code.\nMake sure your LD_LIBRARY_PATH contains \'.\'\n"
				 + e);
		System.exit(1);
	}
}

}

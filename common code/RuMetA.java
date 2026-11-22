package org.coas.ui.bean;

public class RuMetA {

	

		  private static final String VOWELS = "AEIOU";
		  String encodeString = "";	
		  int index = 0;
		  
	    	  
		  public RuMetA(){}  
		  
		  public boolean isRomanMetaphoneEqual(String v1, String v2)
		  {
			  
			  String code1;
			  String code2;
			

			  //.....................................................
			  
			  code1 = encodeRoman(v1);
              code2 = encodeRoman(v2); 
			  
			  if (code1.equalsIgnoreCase(code2))
				  return true;
			  else
				  return false;
		  }
	    
		  
		  public String encodeRoman(String value)
		   {
			  
			  
			  value = cleanInput(value);
			  value = removeConsecDups(value);
			  
			  value  = replaceEmbeddedDigits(value);
		
			  
			  if (value == null) 
			  return null;
	          	   
			  index = 0; 
		      this.encodeString = ""; 
			  
		   
			  while ( index <= value.length() - 1) 
			    {
			      switch (value.charAt(index))
		          {
		          case 'A': 
		          case 'E': 
		          case 'I': 
		          case 'O': 
		          case 'U': 
			   
		        	  index = handleAEIOUY(value,index);
		                 break;
		      //BDFGJLMNPRY
		          case 'B': 
		              this.encodeString = this.encodeString + 'B';
		        	      ++index;
		        	      break;
		      
		          case 'D': 
			    	   this.encodeString += 'D' ;
			    	   ++index;
			    	   break;
			  
			       case 'F': 
			    	   this.encodeString  += 'F';
			    	   ++index;
			    	   break;
			  
			    	   case 'G': 
			    	   this.encodeString = this.encodeString + 'G';
			    	   ++index; 
			    	   break;
			  
			    	   case 'J': 
				    	   this.encodeString  += 'J';
				    	   ++index;
				    	   break;
				    	  
			    		case 'L': 
				       		this.encodeString += 'L';
					        ++index;
					        break;
				       	
					        
				       case 'M': 
				    	   this.encodeString += 'M';
				    	   ++index;
				    	   break;
				    	
				       case 'N': 
				    	   this.encodeString += 'N';
				    	   ++index;
				    	   break;
				    	
					   
				       case 'P': 
				    	   this.encodeString += 'P';
				    	   ++index;
				    	   break;
				    	

				       case 'Y': 
					        this.encodeString  += 'Y';
					    
					        ++index;
					        break;
				       case 'R': 
					    	  this.encodeString += 'R';
					    
				            ++index;
					    	  break;
					      case 'S': 
					    
					    	  this.encodeString += 'S';
					    
					      ++index; 
					    	  break;
					       case 'T': 
					    	   this.encodeString += 'T';
					    
					       ++index;
					    	   break;
				       
					       
				   case 'C': 
			      	   handleC(value, index);
		               break;
			  	   case 'H': 
			    	       handleH(value,index);
			    	       break;
			       case 'K': 
			    	   		this.encodeString += 'K';
			    	   		index = charAt(value, index + 1) == 'Q' ? index + 2 : index + 1;
			    	   		break;
			    	    
			        case 'Q': 
			    	  	     this.encodeString += 'K';
         			    	 index = charAt(value, index + 1) == 'K' ? index + 2 : index + 1;
         			    	 break;
         			    
			    	    case 'V': 
			    	    			this.encodeString = this.encodeString + 'W';
			    	    
			    	    			index = (charAt(value, index + 1) == 'W' ) ? index + 2 : index + 1;
			    	    			break;
			    	    case 'W': 
			    	    			this.encodeString +=  'W';
			    	    
			    	    			index = (charAt(value, index + 1) == 'V' ) ? index + 2 : index + 1;
			    	    			break;
			    	    case 'X': 
			    	    			this.encodeString  += 'Z';
			    	    			index = (charAt(value, index + 1) == 'Z' ) ? index + 2 : index + 1;
			    	    			break;
			         case 'Z': 
			        	 		this.encodeString += 'Z';
			        	
			        	 		index = (charAt(value, index + 1) == 'X' ) ? index + 2 : index + 1;
			        	 		break;
			       default: 
			        
		        } 
		      } 
		    
			  
		    
		    
		    
		    return this.encodeString;
		    
		   } 
		   
		  private int handleAEIOUY(String value, int i)
		   {
		      
			  //.................CASE 1. WE ARE AT THE FIRST CHARACTER..............................................................................
			   
			  if (i == 0) 
		        {
		             if (charAt(value,i) == 'A' || charAt(value,i) == 'E' || charAt(value,i) == 'I')
		             {
			        	  this.encodeString = this.encodeString + 'A';
		              i = (charAt(value, i + 1) == 'A' || charAt(value, i + 1) == 'E' || charAt(value, i + 1) == 'I') ? i + 2 : i + 1;
		              return i;
		             }
		             
		             if (charAt(value, i) == 'U' || charAt(value, i) == 'O')
				          { 
		            	          this.encodeString = this.encodeString + 'U';
					          i = (charAt(value, i + 1) == 'U' || charAt(value, i + 1) == 'O') ? i + 2 : i + 1;
					          return i;
				          }
		                   
		        } 
			   
			  //.............CASE 2. WE ARE AT THE LAST CHARACTER..................................................................................
			   if (i == value.length() - 1 ) 
			   {
				      if (charAt(value,i) == 'A')
				      {
				    	  this.encodeString = this.encodeString + 'A';
				          i = i + 1;
				          return i;
				      }
				      
				      if (charAt(value,i) == 'E' || charAt(value, i) == 'I')
				      {
				    	  this.encodeString = this.encodeString + 'I';
				          i = i + 1;
				          return i;
				      }
				      if (charAt(value,i) == 'O' || charAt(value, i) == 'U')
				      {
				    	  this.encodeString = this.encodeString + 'U';
				          i = i + 1;
				          return i;
				      }
			   }
				
			   //...........IF WE HAVE REACHED HERE THAT MEANS WE ARE ANY WHERE FROM 2ND TO 2ND LAST CHARACTER
			   
			    // we are anywhere from 2nd character to 2nd last. here previous and next character matters
			    	      
			 	  
				   //..........applies to any place.....CASE 3 2ND TO 2ND LAST...............................
			   
				   
				   if (i + 1 < value.length()) 
				      {
					    switch (value.substring(i, i + 2)) 
					    {
					        case "AI":
					        case "AY":
					        case "AE":
					        case "EY":
					        case "IY":
					        case "IA":
					        	this.encodeString += 'Y';  
								   i=i+2;
								   return i;
					      
					        		   
				
					        	}
					
					    if (charAt(value,i) == 'O' || charAt(value, i) == 'U')
					      {
					    	  this.encodeString = this.encodeString + 'U';
					          i = i + 1;
					          return i;
					      }
				      
				      }
				  //...................2nd last character...............................................
				   if (i == value.length() - 2)    
				   {
					   if ((charAt(value, i) == 'A' || charAt(value, i) == 'U') && "AEIOU".indexOf(value.charAt(i+1)) == -1)
					      {
						   i+=1; // igonore A and U
						   return i;
					      }
					   
					   switch (charAt(value, i)) 
					    {
					        case 'U':
					         	this.encodeString += 'U';
				    	     		i = charAt(value, index + 1) == 'O' ? index + 2 : index + 1;
				    	     		return i;
				    	     		
					        case 'O':
					         	this.encodeString += 'U';
				    	   	     	i = charAt(value, index + 1) == 'U' ? index + 2 : index + 1;
				    	   	        return i;
				    	   		 
					    }
				    
		    	       }
				  
			   return ++i;  // means its A,E,I inside the word and we ignore
		   }
		  
		  
		  //............................................................................... 
		  private void handleC(String value,int i)
		   {

		     if (contains(value, i, 2, "CH") && !(contains(value, i-2, 2, "CH")))
	             {
						 this.encodeString = this.encodeString + "CH";
						 this.index = this.index + 2;
						 return ;
		         }
		     if (contains(value, i, 2, "CH") && (contains(value, i-2, 2, "CH")))
             {
					 this.index = this.index + 2;
					 return ;
	         }
		     else
			    {
				
				this.encodeString = (charAt(encodeString,encodeString.length()-1) == 'K') ? this.encodeString : this.encodeString + 'K';
				this.index = this.index + 1;
				}
			    }
		   
		  private void handleH(String value, int i)
		   {
			         
		     if (i == 0) // we are at the first character 
		     {
	       	 	  this.encodeString = this.encodeString + 'H';
	       	 	  this.index = this.index +1;
	       	    if (charAt(value, i + 1) == 'U' || charAt(value, i + 1) == 'A' || charAt(value, i + 1) == 'O')
	       	 	   this.index = this.index + 1;  
	       	    	return ; 	  
		     }
		      
			  if (charAt(value, i - 1) != 'K' &&  charAt(value, i - 1) != 'G' && i != value.length() - 1)
			  {
				  	this.encodeString = this.encodeString + 'H';
			  }
			        
			          this.index = this.index +1;
		      }
		  
		  //........................................................................................................
		  
		  private String cleanInput(String input)
		  {
		    if (input == null) 
		     {
		      return null;
		     }
		    
		    input = input.trim();
		     if (input.length() == 0) 
		     {
		       return null;
		     }
		     return input.toUpperCase();
		 
		  }
		   //.......................................................
		   public static String removeConsecDups(String str) {
			   
            
            
               // 
               /// 
            
			   if (str == null || str.isEmpty()) 
			   { return ""; } 
			// Normalize common Unicode whitespace characters
			   str = str .replace('\u00A0', ' ') // Non-breaking space  
			    .replace('\u2007', ' ') // Figure space  
			    .replace('\u202F', ' ');  // Narrow no-break space  
			    
			   // Normalize multiple spaces into one 
			   str = str.replaceAll("\\s+", " ").trim(); 
			   // Convert to uppercase 
			   str = str.toUpperCase(); 
			   // previous logic
			   
			   if (str == null || str.length() == 0) {
			        return str;
			    }

			   
			    //................................
			    
			    StringBuilder result = new StringBuilder();
			    result.append(str.charAt(0));

			    for (int i = 1; i < str.length(); i++) {
			        if (str.charAt(i) != str.charAt(i - 1)) {
			            result.append(str.charAt(i));
			        }
			    }

			    return result.toString();
			
			
				/*  
               if (str == null || str.isEmpty()) {
				        return "";
				    }

				    // Normalize Unicode whitespace
				    str = str.replace('\u00A0', ' ')
				             .replace('\u2007', ' ')
				             .replace('\u202F', ' ');

				    // Normalize spaces and convert to uppercase
				    str = str.replaceAll("\\s+", " ")
				             .trim()
				             .toUpperCase(java.util.Locale.ROOT);

				    // Collapse repeated CH and GH sequences
				    str = str.replaceAll("(?:CH){2,}", "CH");
				    str = str.replaceAll("(?:GH){2,}", "GH");

				    // Remove consecutive duplicate letters only
				    // Repeated digits remain unchanged
				    StringBuilder result = new StringBuilder();

				    for (int i = 0; i < str.length(); i++) {

				        char current = str.charAt(i);

				        if (result.length() > 0) {

				            char previous = result.charAt(result.length() - 1);

				            if (Character.isLetter(current)
				                    && current == previous) {
				                continue;
				            }
				        }

				        result.append(current);
				    }

				    return result.toString();
				*/
				}
			   
			
		   //.......................................................................................
		   protected char charAt(String value, int index)
		  {
		    if ((index < 0) || (index >= value.length())) {
		       return '\000';
		     }
		     return value.charAt(index);
		   }
		   
		   private static boolean contains(String value, int start, int length, String criteria)
		   {
		     return contains(value, start, length, new String[] { criteria });
		   }
		   
		   private static boolean contains(String value, int start, int length, String criteria1, String criteria2)
		  {
		     return contains(value, start, length, new String[] { criteria1, criteria2 });
		   }
		   
		  private static boolean contains(String value, int start, int length, String criteria1, String criteria2, String criteria3)
		   {
	     return contains(value, start, length, new String[] { criteria1, criteria2, criteria3 });
		   }
		   
		   private static boolean contains(String value, int start, int length, String criteria1, String criteria2, String criteria3, String criteria4)
	  {
		    return contains(value, start, length, new String[] { criteria1, criteria2, criteria3, criteria4 });
	   }
	  
		   private static boolean contains(String value, int start, int length, String criteria1, String criteria2, String criteria3, String criteria4, String criteria5)
		  {
		    return contains(value, start, length, new String[] { criteria1, criteria2, criteria3, criteria4, criteria5 });
		   }
		  
		   private static boolean contains(String value, int start, int length, String criteria1, String criteria2, String criteria3, String criteria4, String criteria5, String criteria6)
	   {
	    return contains(value, start, length, new String[] { criteria1, criteria2, criteria3, criteria4, criteria5, criteria6 });
	   }
		  
		   protected static boolean contains(String value, int start, int length, String[] criteria)
		   {
		    boolean result = false;
		     if ((start >= 0) && (start + length <= value.length()))
		    {
		    String target = value.substring(start, start + length);
	      for (int i = 0; i < criteria.length; i++) {
		        if (target.equals(criteria[i]))
		        {
		          result = true;
	          break;
		         }
		       }
		     }
		     return result;
		   }
//..................new function added................
		   private static String replaceEmbeddedDigits(String input) {

			    if (input == null || input.isEmpty()) {
			        return input;
			    }

			    
			    java.util.Map<String, String> rules =
			            new java.util.HashMap<String, String>();

			    rules.put("1",  "aik");
			    rules.put("2",  "do");
			    rules.put("3",  "ten");
			    rules.put("4",  "char");
			    rules.put("5",  "panch");
			    rules.put("6",  "ch");
			    rules.put("7",  "sat");
			    rules.put("8",  "at");
			    rules.put("9",  "no");

			    rules.put("10", "dus");
			    rules.put("20", "bes");
			    rules.put("30", "tes");


			    
			    java.util.List<String> numbers =
			            new java.util.ArrayList<String>(rules.keySet());

			    java.util.Collections.sort(
			            numbers,
			            new java.util.Comparator<String>() {

			                @Override
			                public int compare(String a, String b) {

			                    return Integer.compare(
			                            b.length(),
			                            a.length()
			                    );
			                }
			            }
			    );


			    String result = input;


			    
			    
				
				for (String number : numbers) {

			        String replacement = rules.get(number);

			        String pattern =
			                "(?<![0-9])" +
			                "(?:" +
			                    "(?<=[A-Za-z])" +
			                    java.util.regex.Pattern.quote(number) +
			                    "|" +
			                    java.util.regex.Pattern.quote(number) +
			                    "(?=[A-Za-z])" +
			                ")" +
			                "(?![0-9])";

			        result = result.replaceAll(
			                pattern,
			                java.util.regex.Matcher.quoteReplacement(replacement)
			        );
			    }


			    
			    result = result.replaceAll(
			            "[^A-Za-z0-9\\s]",
			            ""
			    );


			    return result.toUpperCase();
			}
		   //..........................function ended

    public static void main(String[] args) {
        RuMetA encoder = new RuMetA();
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        while (true) {
            System.out.print("Enter Roman Urdu string: ");
            if (!scanner.hasNextLine()) break;
            String input = scanner.nextLine();
            if (input.trim().isEmpty()) break;
            System.out.println("RuMetA Code: " + encoder.encodeRoman(input));
            System.out.println();
        }
        System.out.println("Program terminated.");
        scanner.close();
    }
}

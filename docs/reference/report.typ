// --- TITLE PAGE CONFIGURATION ---
#set page(
  paper: "a4",
  margin: (top: 3cm, bottom: 2.5cm, left: 3cm, right: 3cm),
)

// Background geometric accent line on the left margin
#place(top + left, dx: -1.5cm, dy: -1cm)[
  #line(length: 90%, angle: 90deg, stroke: 4pt + rgb("1a237e"))
]

#v(2cm)

// Main Title Block
#align(left)[
  #text(size: 14pt, weight: "medium", tracking: 2pt, fill: rgb("757575"))[PROJECT PROPOSAL & REPORT] \
  #v(8pt)
  #text(size: 28pt, weight: "bold", fill: rgb("1a237e"))[Math Jigsaw] \
  #v(4pt)
  #text(size: 16pt, weight: "medium", style: "italic", fill: rgb("2e7d32"))[Making Mathematics Amusing for Younger Students]
]

#v(1.5cm)

// Abstract / Short Description Block
#block(
  width: 100%,
  stroke: (left: 2pt + rgb("b83b3b")),
  inset: (left: 15pt),
  [
    #set text(size: 10pt, style: "italic", fill: rgb("424242"))
    A low-fidelity native application proposal leveraging modern UI components and gamified visual puzzle logic to combat early childhood mathematics anxiety and engage device-centric learners.
  ]
)

#v(5cm)


// Force a page break so the document body starts on page 2
#pagebreak()
// --- END OF TITLE PAGE ---


#outline(title: "Table of Contents")

#pagebreak() 

#set page(
  paper: "a4",
  margin: (x: 1.8cm, y: 1.8cm),
)

#set text(
  font: "New Computer Modern",
  size: 10pt
)

#set par(
  justify: true,
  leading: 0.52em,
)

#align(center)[
  #image("Title.png", width: 20%)
]

#set heading(
  numbering: "I.1.a",
)

// Define your color scheme
#let lvl1-color = rgb("b83b3b") // Red
#let lvl2-color = rgb("2e7d32") // Green

// Enable numbering so the counter functions work
#set heading(numbering: "I.1")

// Show rule to color the ENTIRE heading dynamically
#show heading: it => {
  if it.numbering != none {
    let counts = counter(heading).at(it.location())
    if it.level == 1 {
      // Entire Level 1 title is wrapped in Red
      text(fill: lvl1-color)[
        #numbering("I", ..counts). #it.body
      ]
    } else if it.level == 2 {
      // Entire Level 2 title is wrapped in Green (using only the second number)
      text(fill: lvl2-color)[
        #numbering("1", counts.last()). #it.body
      ]
    } else if it.level == 3 {
      // Entire Level 3 title is wrapped in Blue (using only the third number)
      text(fill: rgb("1a237e"))[
        #numbering("A", counts.last()). #it.body
      ]
    } else {
      it
    }
  } else {
    it
  }
}

= About the project

== Why?

Many studies (@alemany2025attitudes, @tomasetto2020math, @maloney2015intergenerational, @devine2018math) demonstrate that over the years, more and more youth see math as a chore or a source of anxiety.

We want to change that, bringing back the love of numbers and the joy of solving problems to younger students—in the most efficient way possible, of course! 😉

== How?

Studies highlight a shifting educational landscape:

#text(fill: gray.darken(20%), size: 0.85em)[
  _Note: While these global studies highlight widespread trends, observation suggests these behavior patterns heavily translate to the Philippine context as well._
]

+ Adolescents spend an average of ~5.6 hours per day on their smartphones @nagata2025adolescent.
+ Physical textbook usage is decreasing rapidly among modern students @seaman2023k12.
+ Consequently, a large majority of educators bypass assigned textbooks @rand2022materials to source or develop alternative content.

*Conclusion:* Traditional teaching approaches are losing the battle for youth attention.

#v(12pt)
#text(size: 1.2em, weight: "bold")[What can we do about it?]
#v(6pt)  

#grid(
  columns: (1fr, 1fr),
  gutter: 24pt,
  [
    #set text(fill: rgb("b83b3b")) 
    *The Status Quo* \
    #v(4pt)
    Keep complaining about their screen time.
  ],
  [
    #set text(fill: rgb("2e7d32")) 
    *The Opportunity* \
    #v(4pt)
    Meet them where they already are: through technology.
  ]
)

#v(16pt)
#align(center)[
  #text(size: 1.5em, weight: "bold", fill: rgb("1a237e"))[
    Let's change the game. Literally.
  ]
]
#v(6pt)

We are building a *mobile application* designed to turn math into an addictive, puzzle-based game. 

Why a mobile app instead of a desktop platform? Because modern youth carry their phones everywhere. The goal is to occupy the maximum amount of devices possible.

#v(8pt)
#block(
  fill: rgb("e8f5e9"), 
  inset: 15pt,
  radius: 6pt,
  stroke: 1pt + rgb("c8e6c9"),
  width: 100%,
  [
    #set text(fill: rgb("1b5e20"), weight: "bold")
    Our Philosophy:
    #set text(fill: rgb("000000"), weight: "regular")
    Every single second a student spends engaging with our app instead of consuming mindless content is a massive victory for the education system.
  ]
)

== Constraints

Like any projct, we have a few constraints to consider:

+ Budget : We have very limited budget
+ Time : We have a very tight timeline (~3 weeks) to develop a working prototype
+ Resources : We have a small team of 2 developers and 1 designer

We need to make sure that we can deliver a working prototype within the given constraints.

#v(30pt)

= The Game

#pad(left: 1.5em)[
  #set text(style: "italic")
  "A goal without a plan is just a wish."
  
  #set text(style: "normal", weight: "bold")
  --- Antoine de Saint-Exupéry
]

== Game Concept

The first iteration of the game as we envision it , is as follows :

At the beginning , the player is presented with an empty puzzle board, and a quick mental math problem to solve. Once solved properly, the puzzle piece is placed on the board , and the player is presented with another problem. The game continues until the puzzle is completed.

*Why Jigsaw ?* 

We evaluated the different types of puzzles that could be used in the game, and we decided to go with a *jigsaw puzzle*. This is because jigsaw puzzles are easy to understand and can be completed in a short amount of time, which is ideal for simple mobile sessions.

The point is to make the game as *simple , intuitive and pleasing* as possible, so that the players never get bored or frustrated. The game should be a fun and engaging way to practice math skills, and we believe that a jigsaw puzzle is the perfect way to achieve this.

In that sense, as we'll see in the next parts, we spend a lot of time thinking about user interface and experience, as well as the different mechanics and effects we can use to make the game more engaging and fun.

Because the real *challenge* here, is that we have to get students to play the game - and keep them playing -, which means literally means *competing with the other apps on their phones*, which are sadly way more stimulating and addictive, such as TikTok, Instagram, etc...
#text(fill: gray.darken(20%), size: 0.85em)[
_ Which of course aren't as good for their brains as our math game _
]


== Sketch (Hand-Drawn)

We then drew some sketches of how the features we describe previously could be presented in an application

#figure(
  grid(
    columns: (1fr, 1fr, 1fr, 1fr), // 4 equal columns
    gutter: 10pt,                  // Space between the images
    image("sketch1.png", width: 100%),
    image("sketch2.png", width: 100%),
    image("sketch3.png", width: 100%),
    image("sketch4.png", width: 100%),
  ),
  caption: [A side-by-side look at the different in-game screens.],
)

== Mockups (AI-Generated)

Here's how the user interface could look like, that the AI generated a visual for based on our hand sketches. The AI-generated mockups are not final, but they give us a good idea of how the app could look like.

Indeed, it's important to have an idea of the final product before starting the work any project.

They key takeaway from these mockups, is how colors, shapes and simple effects can turn a sketch into an attractive game interface, and so that after making the core of the game, we will have to do the same too !

#figure(
  grid(
    columns: (1fr, 1fr, 1fr, 1fr), // 4 equal columns
    gutter: 10pt,                  // Space between the images
    image("JigsawMathTitle.png", width: 100%),
    image("JigsawMathGame.png", width: 100%),
    image("JigsawMathGallery.png", width: 100%),
    image("JigsawMathSettings.png", width: 100%),
  ),
  caption: [A side-by-side look at the AI-Generated user interface.],
)


= Technical Details

== Technology Stack

Now that we have a clear understanding of the problem, let's discuss the technical details of our solution.

We want to make a mobile application, however there's currently 2 types of phones on the market - Android (~70% market share) and iOS (~30% market share). iOs is what powers iPhones, while Android powers a wide variety of devices from different manufacturers - Samsung, Huawei, Xiaomi, Oppo, etc.

#figure(
  image("Apple_vs_Android.png", width: 50%),
  caption: "The eternal debate - Apple vs Android"
)

To deliver a working prototype within our tight constraints, we evaluated three potential development paths based on implementation speed, learning curve, and final output quality:

#v(8pt)
#table(
  columns: (1.2fr, 2fr, 1fr),
  fill: (x, y) => if y == 0 { rgb("f0f0f0") } else if y == 1 { rgb("e8f5e9") } else { none },
  align: (col, row) => if col == 0 { left } else { center },
  stroke: 0.5pt + rgb("dddddd"),
  
  [*Approach*], [*Pros & Cons*], [*Learning Curve*],
  [*Native Android* \ (Kotlin + Compose)], [Targets android only. Jetpack Compose allows rapid UI development with modern, declarative code.], [*Fast* \ (Excellent documentation and resources available)],
  [*Cross-Platform* \ (Flutter / React Native)], [Reaches both iOS and Android, but requires learning an entirely new ecosystem and configuration from scratch.], [*Steep* \ (Too risky for a 3-week sprint)],
  [*MIT App Inventor*], [Extremely fast block-based prototyping, but highly restrictive for building a polished, complex game architecture.], [*Immediate* \ (Too limited for scaling)],
  [*XCode / SwiftUI*], [Targets iOS only. SwiftUI allows rapid UI development with modern, declarative code.], [*Fast* \ (Excellent documentation and resources available, but requires a Mac to develop and test + fees)],
)
#v(8pt)

#block(
  fill: rgb("e3f2fd"), // Light blue informational block
  inset: 12pt,
  radius: 4pt,
  stroke: 1pt + rgb("bbdefb"),
  width: 100%,
  [
    #set text(fill: rgb("0d47a1"), weight: "bold")
    Retained Approach: Native Android (Kotlin & Jetpack Compose)
    
    #set text(fill: rgb("000000"), weight: "regular")
    Given our strict *3-week timeline* and small team, maximizing development speed is paramount. While MIT App Inventor offers rapid block prototyping, it lacks the flexibility needed for a game layout. Cross-platform options present too steep a learning curve for a short sprint. Therefore, we retained *Kotlin with Jetpack Compose*—allowing us to leverage abundant, high-quality educational resources to quickly deploy a responsive native app for the majority market share.
  ]
)

== Tools

Since we are developing a mobile application, we need to use the following tools:

- Android Studio : The official IDE for Android development. It provides a complete set of tools for building, testing, and debugging Android apps.

#figure(
  image("android-studio.png", width: 50%),
  caption: "Android Studio - The official IDE for Android development"
)

- GitHub : A web-based platform for version control and collaboration. It allows us to manage our codebase, track changes, and collaborate with team members.

#figure(
  image("repo.png", width: 70%),
  caption: "GitHub - A web-based platform for version control and collaboration"
)

- AI : We will leverage AI tools to assist in code generation, debugging, and optimization. This will help us speed up development and ensure high-quality code.

#figure(
  image("example_ai.png", width: 70%),
  caption: "AI - Leveraging AI to learn app development."
)


== Developement Environment

There are various operating systems available for different types of computers. The PCs we had access to were running *Windows*..

Microsoft defines Windows as a general-purpose operating system designed to run on personal computers, including desktops, laptops, and tablets. It provides a graphical user interface (GUI), virtual memory management, multitasking capabilities, and support for many peripheral devices.

#figure(
  grid(
    columns: (1fr, 1fr),
    gutter: 10pt,
    image("windows10.png", width: 100%),
    image("windows11.png", width: 100%),
  ),
  caption: [Windows 10 and Windows 11],
)


== Installing Android Studio

We install Android Studio through the official website #link("https://developer.android.com/studio").

All we have to do is download the installer, and follow the instructions. We helped ourselves with this youtube video #link("https://www.youtube.com/watch?v=XmmMXYBzlns")


#figure(
  image("android_studio_splash.png", width: 70%),
  caption: "Android Studio - Splash screen on launch"
)


== Programming Language

Android studio supports multiple programming languages and frameworks, but we will be using *Kotlin* for our project.

See #text(fill: rgb("b83b3b"))[@Kotlin] for more details



= Kotlin & Jetpack Compose <Kotlin>

== History of Android Development

For many years, Android development was primarily done using Java, a widely-used programming language known for its portability and robustness.
It powers a wide range of applications, from web servers, mobile apps to games like Minecraft.

However, it presented some challenges for Android developers, and the language was often criticized for encouraging old programming practices that could lead to less efficient and more error-prone code.
So in 2017, Google, the company behind Android, announced Kotlin as an officially supported language for Android development, aswell as a new ecosystem that accompanies it called Jetpack Compose.

Basically, developers went from visually designing their apps using a visual editor, and making it interactive with code, to literally writing the entire app in code, including the user interface.

As you can see in the image below, on the left, we can see how developers back in the day had an editor to visually add an place buttons, text fields, images and other elements. 

However, on the right, we can see how developers now have to write code to create the same user interface, and then preview it in a separate window.

#figure(
  grid(
    columns: (1fr, 1fr),
    gutter: 10pt,
    image("xml_editor.png", width: 100%),
    image("compose_preview.png", width: 100%),
  ),
  caption: [On the left, the old XML-based UI editor. On the right, the new Jetpack Compose preview.],
)

== About Kotlin

As said before, modern Android development is primarily done by writing code in the Kotlin programming language, so we will be using it for our project too !


== Using AI to accelerate development

The use of AI in software development has become increasingly prevalent, offering a range of benefits that can significantly enhance the development process. 

The latest Android Studio version that we used for our project, includes an AI-powered code assistant that can help developers write code, and even autonomously generate hundred of lines of code based on a human prompt.

Given our tight timeline, and to maximize our productivity, we decided to use it too, since we live in a world where economy rewards the fastest actors, and not specially the most skilled ones...

Below , we present a sample interaction with the AI code assistant, where we asked it to create a visual component, based on text instructions and a screenshot :

#figure(
  grid(
    columns: (1fr, 1fr),
    gutter: 10pt,
    image("example_ai_prompt.png", width: 100%),
    image("result_prompt.png", width: 70%),
  ),
  caption: [Prompt we gave to the AI code assistant on the Left, and the result it generated on the Right],
)

After a few minutes, and a few prompts to further customize our request, we got the result on the right, which was fully working, had no errors and ran perfectly well.

We can clearly see it's not 1:1 with our mockup, and it is not as polished as we would like it to be, but it is a good starting point. The complicated technical details of the code are handled by the AI, and we can focus on the design and user experience instead !

We automated technical parts of the code, that would otherwise would have taken us hours or days to learn and do manually, which is what companies like Google, Microsoft, and OpenAI are doing to accelerate software development and reduce costs in real life scenarios.

= Publishing


In order for Android smartphone users to be able to find and download our application, we have to send it to an *Application Store*, which is an *online* service that acts as a sort of catalog for users to safely browse, install, and update apps on their phones. You can think of it like a big city mall where everything is neatly organize and ready for you to get.


#figure(
  grid(
    columns: (1fr,1fr, 1fr),
    gutter: 10pt,
    image("app_store.png", width: 90%),
    image("play_store.png", width: 80%),
    image("fdroid.png", width:80%),
  ),
  caption: [Example of Application Stores : *iOS App Store*, Android *Play Store* and *F-Droid* Store]
)

== What's "Publishing" ?

In plain terms, *publishing* an app means *making it available to the public*. When developers finish building an app, it exists as a single file on their computer. "Publishing" is the process of uploading that file to a marketplace so that everyday users can find it, click "Install," and have it safely placed onto their devices.

Even if we didn't have enough time to really consider *publishing* the application, we still studied the different possibilities regarding that matter. More precisely, we focused on two marketplaces : "Google Play Store" and "F-Droid". Because they are the simplest , most accessible and widespread.

== Google Play store

The Google Play Store is the "official" and most popular digital storefront on Android devices. It comes pre-installed on almost every Android phone in the world.

#grid(
  columns: (1fr, 1fr),
  gutter: 20pt,
  [
    === Pros
    - *Massive Audience:* It has billions of active users. If you want the most people possible to see your app, this is where you go.
    - *Ultimate Convenience:* Users don't have to change any settings on their phones to use it. It also handles automatic updates seamlessly.
    - *Built-in Security Checking:* Google runs automated scans on apps to catch malware before it reaches a user's phone.
  ],
  [
    === Cons
    - *Cost and Hurdles:* Google charges a one-time registration fee (25 USD) for developers and takes a percentage cut of any money the app makes. 
    - *Strict Corporate Rules:* Google has strict policies regarding what an app can do, and they can remove your app from the store if rules change.
    - *Privacy Trade-offs:* Google tracks a lot of user data and download habits through the platform.
    - *2026 Changes*: Google changed the process, and there's even more complicated steps now. (ID Verification + human testing etc)
  ]
)


== Option B: F-Droid
F-Droid is an alternative, independent app catalog. It is entirely non-profit and focuses exclusively on *Free and Open-Source Software (FOSS)*. 

#grid(
  columns: (1fr, 1fr),
  gutter: 20pt,
  [
    === Pros
    - *Strict Privacy:* F-Droid does not track what users download, doesn't require a Google account, and bans apps that contain hidden tracking or heavy advertising.
    - *Open and Transparent:* Every app on F-Droid must show its "recipe" (its source code) openly. This allows security experts to verify that the app isn't doing anything sneaky behind the scenes.
    - *Completely Free:* There are no fees to publish or download apps.
  ],
  [
    === Cons
    - *Harder for Average Users to Install:* Because Google doesn't allow competing app stores inside the Play Store, users have to manually download F-Droid from a website and change a setting on their phone to allow "Unknown Sources."
    - *Much Smaller Audience:* F-Droid is mostly used by tech-savvy people or privacy enthusiasts. It has thousands of apps compared to Google's millions.
    - *Slower Updates:* Because human volunteers manually check and compile the code for safety, app updates can take a few days or weeks to show up compared to the Play Store.
  ]
)

== Choice

Given the fact, that our app is intended to be educational, we have three possibilities:
- make it free
- make it paid
- make it free but integrate in app-purchases, just like free-to-play videogames.

Currently, the application is more of a proof-of-concept, than really a final product that we could integrate paid features into, so we found out that if we had to publish it in it's current state, it shouldve been into F-Droid, since there's no publishing fees, and we don't need the financial features given by Google Play Store. Addtionally ,on F-Droid, potential users can download it for free and give feedback in order for us to improve the application, as well as contribute to our project.


= Difficulties

During the creation of the app, we faced so many difficulties

== The tools are so heavy

The tool that we used, Android Studio, is so heavy and complex , it requires having a powerful computer, and a very high network bandwidth to work properly.

Since the computer we had access to, wasn't powerful enough, we spent a lot of time waiting, even on the simplest of tasks such as changing small parts of code. It made us waste a lot of time and energy, and the developement would probably have not been possible without the AI developement features.

== Not all phone brands are compatible with android studio

There is a feature in android studio, to test the application in real-time on a phone , to see exactly if it works or not on the spot, and any change would result in an immediate change on the device.

However, none of our phones were compatible with that feature, so instead we had to manually convert the application to a format that the phone can install, and then find a way to send it and install it. It cost us a lot of time and energy, since it means that every time a feature was added, we couldnt simply see the result, but had to follow this long process in order to be able to see the result of our changes.


= Glossary

Below is the explanation of some technical terms that were used in this report !

// Switch to a highly readable sans-serif font for the glossary entries
#set text(font: "Liberation Sans", size: 9.5pt)
#set par(leading: 0.65em) // Slightly increased spacing between lines for breathing room

#v(12pt)

// --- FOUNDATIONAL CONCEPTS ---

#block(stroke: (left: 3pt + rgb("1a237e")), inset: (left: 12pt, y: 2pt))[
  *Software* \
  Physical computer components (the screen, battery, microchips) are the physical body of a device.
  
   A software is a set of instructions that tells these physical components how to behave.
   
   I.e : *Apps*, *mobile games*, *web browsers*, and operating systems are all different types of software.
]

#figure(
  image("example_software.png", width: 50%),
  caption: "Example of softwares that you may know"
)

#v(14pt)

#block(stroke: (left: 3pt + rgb("1a237e")), inset: (left: 12pt, y: 2pt))[
  *Coding / Programming* \
  Computers do not natively understand human languages like English or Tagalog; they only understand basic electrical signals (on or off). *Coding* is the act of writing step-by-step recipes or instructions in a specialized language (like Kotlin) that bridges the gap, translating human ideas into commands a computer can execute.
]

#figure(
  image("example_mobile_languages.png", width: 50%),
  caption: "Your favorite app might be written in one of these programming languages"
)

#v(14pt)

#block(stroke: (left: 3pt + rgb("b83b3b")), inset: (left: 12pt, y: 2pt))[
  *Bug* \
  A fancy word for a mistake, error, or flaw in a software program that causes it to behave unexpectedly. This could range from a minor visual glitch (like text overlapping) to a severe crash (the app shutting down entirely). "Debugging" is the detective work developers do to hunt down and fix these errors.
]

#v(14pt)

#block(stroke: (left: 3pt + rgb("2e7d32")), inset: (left: 12pt, y: 2pt))[
  *Framework* \
  Think of a framework as a prefabricated house kit. Instead of forcing a carpenter to cut down trees and forge custom nails just to build a kitchen, the kit provides pre-made walls and structures. In software, a framework (like Jetpack Compose) provides built-in components for common features like scroll lists, buttons, and text fields so developers don't have to build them from absolute scratch.

  In short, it's a set of tools and pre-written code that helps developers build applications more efficiently, ensuring consistency and reducing the likelihood of errors.
]

#v(14pt)

// --- ADVANCED ECOSYSTEM CONCEPTS ---

#block(stroke: (left: 3pt + gray.darken(20%)), inset: (left: 12pt, y: 2pt))[
  *Operating System (OS)* \
  The master software that acts as the supervisor of a device. It manages the physical hardware resources and allocates them to the apps you run. It is the reason an app can easily request to use your phone's screen or processor without needing to know the technical engineering details of that specific phone model.
]

#v(14pt)

#block(stroke: (left: 3pt + gray.darken(20%)), inset: (left: 12pt, y: 2pt))[
  *IDE (Integrated Development Environment)* \
  The ultimate digital workshop for a software developer. It is a specialized text editor (like Android Studio) that highlights typos in your code, auto-completes lines, warns you about potential bugs, and features a built-in virtual smartphone to test your game safely on your computer screen.
]

#v(14pt)

#block(stroke: (left: 3pt + gray.darken(20%)), inset: (left: 12pt, y: 2pt))[
  *Native vs. Cross-Platform Applications* \
  *Native apps* are coded exclusively for one type of device operating system using its official language (e.g., using Kotlin for Android). They run blindingly fast and fit perfectly on the phone, but won't work anywhere else. *Cross-platform apps* use a translation system to write code once and run it on both iPhones and Androids, which saves time but can slow down animation-heavy visual games.
]

#v(14pt)

#block(stroke: (left: 3pt + gray.darken(20%)), inset: (left: 12pt, y: 2pt))[
  *Version Control (Git / GitHub)* \
  A shared digital safety net for programmers. It tracks every single line of code edited by the team chronologically. If someone writes code that accidentally breaks the entire game, Version Control allows the team to instantly hit a massive "Undo" button and restore the app to a perfectly functional state from earlier in the day.
]

#v(14pt)

#block(stroke: (left: 3pt + gray.darken(20%)), inset: (left: 12pt, y: 2pt))[
  *Low-Fidelity Wireframes & Mockups* \
  A *wireframe* is a quick, rough design sketch (like drawing on a napkin or using an online sketching board) focused entirely on where elements sit, completely ignoring colors or fine art. A *mockup* is a polished static image (often generated to look real) showing stakeholders what the final product will visually look like before a single line of actual game code is written.
]

#v(10pt)
#bibliography("references.bib")
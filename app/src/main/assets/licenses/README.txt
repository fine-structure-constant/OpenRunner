Third-party font notice
=======================

Noto Serif SC
-------------

  Upstream family : Noto Serif SC
  Upstream source : NotoSerifSC[wght].ttf, version 2.002 (variable, wght axis)
                    Google Fonts, https://fonts.google.com/noto/specimen/Noto+Serif+SC
  Copyright       : (c) 2017-2024 Adobe (http://www.adobe.com/).
  Designer        : Google / Adobe
  License         : SIL Open Font License 1.1 - see NotoSerifSC-OFL.txt in this folder
  License URL     : http://scripts.sil.org/OFL

What ships in this app
----------------------

Two static subsets are bundled as Android font resources:

  res/font/or_serif_regular.ttf    instance wght=400
  res/font/or_serif_semibold.ttf   instance wght=600

Both are Modified Versions within the meaning of the license. The modifications are:

  1. The variable axis is frozen to a single weight (a static instance).
  2. The glyph set is subset to the characters the UI actually uses.
  3. Digits 0-9 are redrawn to a common advance width and optically centred,
     so a ticking timer does not shift horizontally. The upstream family
     exposes no 'tnum' feature, so this could not be done through layout tags.
  4. The family name is changed to "OpenRunner Serif".

Because of (4) no Reserved Font Name is used, which satisfies condition 3 of
the license. The upstream copyright, trademark, license and license-URL records
are retained inside the font binaries (name IDs 0, 7, 13 and 14) so they remain
machine-readable, satisfying condition 2.

Reproducing the subsets
-----------------------

    python tools/build_serif_font.py

The script reads the upstream variable font from the system font directory and
rewrites both resources. Re-run it after adding UI text, then run the unit test
suite: FontCoverageTest fails when the UI uses a character the subset lacks.

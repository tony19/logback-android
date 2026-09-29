#!/usr/bin/env python3
#
# Lists what a JaCoCo XML report leaves uncovered, one source file per line:
#
#   ch/qos/logback/core/FileAppender.java: missed lines 120-122; partly-missed
#   lines 88; missed branches L88(1/2)
#
# "L88(1/2)" means one of the two branches on line 88 never ran. The last line
# is the total. logback-android's coverage gate (verifyCoverage) requires every
# line and branch of the library to be covered, so this is the list of what a
# failing gate is missing, without downloading the HTML report.
#
# Run: ./gradlew coverageReportJdk11Debug
#      scripts/coverage-gaps.py logback-android/build/reports/jacoco/coverageReportJdk11Debug/coverageReportJdk11Debug.xml

import re
import sys
import xml.etree.ElementTree as ET


def ranges(numbers):
    """[1, 2, 3, 7] -> '1-3,7'"""
    spans = []
    for n in sorted(numbers):
        if spans and n == spans[-1][1] + 1:
            spans[-1][1] = n
        else:
            spans.append([n, n])
    return ','.join(str(a) if a == b else f'{a}-{b}' for a, b in spans)


def gaps(report):
    with open(report, encoding='utf-8') as f:
        # JaCoCo's DOCTYPE names a DTD that isn't shipped with the report
        root = ET.fromstring(re.sub(r'<!DOCTYPE[^>]*>', '', f.read(), count=1))
    totals = {'LINE': [0, 0], 'BRANCH': [0, 0]}
    for counter in root.findall('counter'):
        if counter.get('type') in totals:
            totals[counter.get('type')] = [int(counter.get('missed')), int(counter.get('covered'))]
    files = []
    for package in root.iter('package'):
        for source in package.findall('sourcefile'):
            missed, partly, branches = [], [], []
            for line in source.findall('line'):
                nr, mi, ci, mb, cb = (int(line.get(k)) for k in ('nr', 'mi', 'ci', 'mb', 'cb'))
                if mi and not ci:
                    missed.append(nr)
                elif mi:
                    partly.append(nr)
                if mb:
                    branches.append(f'L{nr}({mb}/{mb + cb})')
            parts = []
            if missed:
                parts.append(f'missed lines {ranges(missed)}')
            if partly:
                parts.append(f'partly-missed lines {ranges(partly)}')
            if branches:
                parts.append('missed branches ' + ', '.join(branches))
            if parts:
                files.append(f"{package.get('name')}/{source.get('name')}: " + '; '.join(parts))
    return sorted(files), totals


def main(argv):
    if len(argv) != 2:
        sys.exit(f'usage: {argv[0]} <jacoco-report.xml>')
    files, totals = gaps(argv[1])
    for line in files:
        print(line)
    (lm, lc), (bm, bc) = totals['LINE'], totals['BRANCH']
    print(f'{len(files)} file(s) with gaps; lines {lc}/{lc + lm} covered, '
          f'branches {bc}/{bc + bm} covered')


if __name__ == '__main__':
    main(sys.argv)

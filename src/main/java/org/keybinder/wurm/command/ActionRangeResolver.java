package org.keybinder.wurm.command;

/**
 * Client-side snapshot of {@code ActionEntry.getRange()} from the Wurm Unlimited
 * server build paired with this client. PlayerAction does not carry this value.
 */
public final class ActionRangeResolver {
    private static final float DEFAULT_RANGE = 4.0f;

    public float actionRange(short actionId) {
        switch (actionId) {
            case 211: case 505: case 506:
                return 1.0f;
            case 98: case 169: case 170: case 379:
                return 2.0f;
            case 348: case 349: case 350:
                return 3.0f;
            case 3: case 4: case 6: case 99: case 103: case 105: case 114:
            case 177: case 178: case 181: case 197: case 198: case 199:
            case 200: case 201: case 202: case 203: case 204: case 205:
            case 206: case 207: case 208: case 257: case 274: case 275:
            case 287: case 288: case 289: case 290: case 291: case 292:
            case 293: case 294: case 295: case 296: case 297: case 298:
            case 299: case 300: case 301: case 302: case 303: case 304:
            case 305: case 306: case 307: case 308: case 309: case 310:
            case 311: case 312: case 313: case 314: case 315: case 316:
            case 317: case 340: case 374: case 375: case 504: case 696:
            case 697: case 864: case 925: case 926:
                return 6.0f;
            case 11: case 196: case 209: case 210: case 319: case 320:
            case 321: case 322: case 323: case 324: case 354: case 355:
            case 356: case 365: case 366: case 367: case 368: case 453:
            case 490:
                return 8.0f;
            case 258: case 496: case 497: case 498: case 499: case 500:
            case 501: case 502: case 503:
                return 10.0f;
            case 246: case 247: case 248: case 249: case 250: case 252:
            case 254: case 331: case 332: case 351: case 425: case 445:
            case 605: case 606: case 701: case 702: case 703: case 929:
                return 12.0f;
            case 346: case 357: case 364:
                return 16.0f;
            case 352: case 359: case 360: case 361: case 362: case 422:
            case 745:
                return 20.0f;
            case 413: case 414: case 418: case 420: case 432: case 433:
            case 450: case 547: case 548: case 549: case 550: case 551:
            case 552: case 554: case 555: case 556: case 557: case 558:
            case 559: case 560: case 561: case 629: case 630: case 631:
            case 634: case 686: case 946:
                return 24.0f;
            case 60: case 61: case 226: case 334:
                return 30.0f;
            case 160: case 343: case 344: case 369: case 406: case 412:
            case 417: case 419: case 436: case 437: case 440: case 512:
            case 513: case 934:
                return 40.0f;
            case 255: case 424: case 426: case 428: case 429: case 430:
            case 431: case 435: case 485: case 641: case 931: case 932:
                return 50.0f;
            case 40: case 42:
                return 80.0f;
            case 179: case 180:
                return 100.0f;
            case 124: case 125: case 126: case 127: case 128: case 129:
            case 130: case 131: case 134: case 326: case 327: case 329:
            case 341: case 342: case 483: case 486: case 494: case 495:
            case 510: case 716:
                return 200.0f;
            case 469: case 470: case 471: case 472: case 476: case 577:
            case 578: case 579: case 580:
                return 300.0f;
            default:
                return DEFAULT_RANGE;
        }
    }

    public float scanRange(short actionId, float configuredMinimum) {
        return Math.max(actionRange(actionId) * 2.0f, configuredMinimum);
    }

    public boolean isWithinActionRange(short actionId, double squaredDistance) {
        float range = actionRange(actionId);
        return squaredDistance <= range * range;
    }
}

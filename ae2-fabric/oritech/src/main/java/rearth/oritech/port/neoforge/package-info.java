/**
 * Fabric replacements for the parts of NeoForge's API that Oritech uses, with the same class and method names so that
 * Oritech's code needs few changes. They are written from scratch on top of Fabric API (transfer API, API lookups,
 * networking, events) and Team Reborn Energy. Fluid amounts stay in millibuckets inside Oritech and are converted to
 * droplets (81 per millibucket) where they meet Fabric's fluid API.
 */
package rearth.oritech.port.neoforge;

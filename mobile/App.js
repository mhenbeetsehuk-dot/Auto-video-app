import React,{useState} from "react";
import {View,Text,Pressable,StyleSheet,Alert,ActivityIndicator,ScrollView,Linking} from "react-native";
import * as ImagePicker from "expo-image-picker";
import {File} from "expo-file-system";
import {fetch as expoFetch} from "expo/fetch";

const API=process.env.EXPO_PUBLIC_API_URL||"http://localhost:8000";

const platforms=[
 {id:"youtube",label:"YouTube Shorts",icon:"▶"},
 {id:"tiktok",label:"TikTok",icon:"♪"},
 {id:"instagram",label:"Instagram Reels",icon:"◎"},
 {id:"facebook",label:"Facebook Reels",icon:"f"}
];

export default function App(){
 const [video,setVideo]=useState(null),[busy,setBusy]=useState(false),[interval,setIntervalHours]=useState(3),[mode,setMode]=useState("automatic");

 async function pickVideo(){
   const permission=await ImagePicker.requestMediaLibraryPermissionsAsync();
   if(!permission.granted){Alert.alert("Permission required","Allow GameClip Publisher to access your videos in Settings.");return;}
   const r=await ImagePicker.launchImageLibraryAsync({mediaTypes:["videos"],allowsEditing:false,quality:1,videoExportPreset:"Passthrough"});
   if(!r.canceled)setVideo(r.assets[0]);
 }

 async function connect(platform){
   try{
     await Linking.openURL(API+"/auth/"+platform);
   }catch(e){
     Alert.alert("Connection unavailable","Set EXPO_PUBLIC_API_URL to your publishing server, then try again.");
   }
 }

 async function createClips(){
   if(!video){Alert.alert("Choose a video first");return}
   setBusy(true);
   try{
     const file=new File(video.uri);
     if(!file.exists)throw new Error("The selected video cannot be read. Please grant Photos/Videos permission and try again.");
     const f=new FormData();
     f.append("video",file);
     f.append("mode",mode);
     f.append("schedule_interval_hours",String(interval));
     f.append("output_resolution","2160x3840");
     f.append("aspect_ratio","9:16");
     f.append("music_mode","automatic");
     f.append("auto_delete_posted_clips","true");
     f.append("auto_delete_source","false");
     const r=await expoFetch(API+"/api/create-clips",{method:"POST",body:f});
     const j=await r.json();
     if(!r.ok)throw new Error(j.detail||"Processing failed");
     Alert.alert("Automatic job started",`${(j.clips||[]).length||"Your"} clips were queued for automatic processing and publishing.`);
   }catch(e){Alert.alert("Could not start job",String(e.message||e))}
   finally{setBusy(false)}
 }

 async function cleanUp(){
   Alert.alert("Clean Up source video","This removes the selected source video and temporary local files. Posted clips are already removed automatically.",[
    {text:"Cancel",style:"cancel"},
    {text:"Clean Up",style:"destructive",onPress:async()=>{
      try{
       if(video?.uri) await new File(video.uri).delete();
       setVideo(null);
      }catch(e){Alert.alert("Clean Up failed",String(e.message||e))}
    }}
   ]);
 }

 return <View style={s.bg}><ScrollView contentContainerStyle={s.body}>
   <Text style={s.title}>GAMECLIP PUBLISHER</Text>
   <Text style={s.sub}>Automatic 4K gaming shorts</Text>

   <Text style={s.h}>1. Connect accounts</Text>
   <Text style={s.note}>Tap a platform to sign in and authorize publishing. Your passwords stay with the platform.</Text>
   {platforms.map(p=><Pressable key={p.id} style={s.account} onPress={()=>connect(p.id)}>
     <Text style={s.icon}>{p.icon}</Text><View style={{flex:1}}><Text style={s.accountTitle}>{p.label}</Text><Text style={s.accountSub}>Connect account</Text></View><Text style={s.arrow}>›</Text>
   </Pressable>)}

   <Text style={s.h}>2. Automatic mode</Text>
   <Pressable style={[s.mode,s.modeActive]}><Text style={s.white}>⚡ AUTOMATIC — END TO END</Text><Text style={s.modeSub}>Clips, music, 9:16, 4K, captions, queue and publishing</Text></Pressable>

   <Text style={s.label}>POSTING INTERVAL</Text>
   <View style={s.row}>{[1,2,3,4,6,8,10].map(n=><Pressable key={n} onPress={()=>setIntervalHours(n)} style={[s.chip,interval===n&&s.active]}><Text style={s.white}>{n}h</Text></Pressable>)}</View>

   <Text style={s.h}>3. Choose gameplay video</Text>
   <Pressable style={s.pick} onPress={pickVideo}><Text style={s.white}>{video?.fileName||"📁 Choose gameplay video"}</Text></Pressable>
   {video&&<Text style={s.fileNote}>Output target: 2160 × 3840 • 9:16 • highest practical quality</Text>}

   {video&&<Pressable style={s.cleanup} onPress={cleanUp}><Text style={s.white}>🧹 CLEAN UP SOURCE VIDEO</Text></Pressable>}

   {busy?<ActivityIndicator color="#fff" style={{marginTop:28}}/>:<Pressable style={s.primary} onPress={createClips}><Text style={s.white}>🚀 START AUTOMATIC PROCESS</Text></Pressable>}
   <Text style={s.footer}>Posted clips are removed automatically after successful publication. The source video stays until you press Clean Up.</Text>
 </ScrollView></View>
}
const s=StyleSheet.create({
 bg:{flex:1,backgroundColor:"#0b0f14"},body:{padding:22,paddingTop:65,paddingBottom:50},
 title:{color:"#fff",fontSize:27,fontWeight:"900"},sub:{color:"#8c97a5",marginTop:6},
 h:{color:"#fff",fontSize:24,fontWeight:"800",marginTop:28,marginBottom:14},
 account:{backgroundColor:"#151b23",borderRadius:14,padding:16,marginBottom:9,flexDirection:"row",alignItems:"center"},
 icon:{color:"#fff",fontSize:24,width:36,textAlign:"center"},accountTitle:{color:"#fff",fontWeight:"800"},accountSub:{color:"#8792a0",marginTop:3,fontSize:12},arrow:{color:"#8c97a5",fontSize:30},
 note:{color:"#7e8996",lineHeight:20,marginBottom:14},
 mode:{backgroundColor:"#151b23",borderRadius:14,padding:17,borderWidth:1,borderColor:"#2f81f7"},modeActive:{backgroundColor:"#111c2b"},modeSub:{color:"#9aa6b4",marginTop:7,lineHeight:19},
 label:{color:"#8792a0",fontSize:12,fontWeight:"700",marginTop:20,marginBottom:8},row:{flexDirection:"row",gap:8,flexWrap:"wrap"},
 chip:{backgroundColor:"#171e27",padding:12,borderRadius:20},active:{backgroundColor:"#2f81f7"},
 pick:{backgroundColor:"#151b23",borderRadius:14,padding:20},fileNote:{color:"#7e8996",fontSize:12,marginTop:9},
 cleanup:{backgroundColor:"#171e27",padding:15,borderRadius:12,marginTop:16,alignItems:"center"},
 primary:{backgroundColor:"#2f81f7",padding:17,borderRadius:13,alignItems:"center",marginTop:25},
 white:{color:"#fff",fontWeight:"700"},footer:{color:"#687482",fontSize:12,lineHeight:18,marginTop:22,textAlign:"center"}
});